package com.kercer.kerkee.net;

import java.io.IOException;

import org.apache.http.HttpHost;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRoute;
import org.apache.http.conn.params.ConnRouteParams;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.CookieSpec;
import org.apache.http.cookie.CookieSpecFactory;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.impl.cookie.BrowserCompatSpec;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HttpContext;

import com.kercer.kerkee.log.KCLog;

import android.net.Proxy;

/**
 * 
 * @author zihong
 *
 */
public class KCHttpClient
{
    private static KCHttpClient mInstance;
    private static String mCustomUA;

    private DefaultHttpClient mHttpClient;

    public static KCHttpClient getInstance()
    {
        return mInstance;
    }

    private static void createHttpClient()
    {
        mInstance = new KCHttpClient();

        HttpRequestRetryHandler retryHandler = new HttpRequestRetryHandler()
        {
            @Override
            public boolean retryRequest(IOException e, int executionCount, HttpContext context)
            {
                // retry at most 5 times
                return executionCount < 5;
            }

        };

        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, 30000);
        HttpConnectionParams.setSoTimeout(params, 30000);
        HttpConnectionParams.setSocketBufferSize(params, 8192);
        HttpConnectionParams.setTcpNoDelay(params, true);
        HttpConnectionParams.setStaleCheckingEnabled(params, false);

        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, "utf-8");
        HttpProtocolParams.setUserAgent(params, getCustomUA(false));
        HttpProtocolParams.setUseExpectContinue(params, false);

        ConnManagerParams.setMaxTotalConnections(params, 6);
        ConnManagerParams.setMaxConnectionsPerRoute(params, new ConnPerRoute()
        {
            @Override
            public int getMaxForRoute(HttpRoute httpRoute)
            {
                return 6;
            }
        });

        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        ThreadSafeClientConnManager connManager = new ThreadSafeClientConnManager(params, registry);

        DefaultHttpClient httpclient = new DefaultHttpClient(connManager, params);

        CookieSpecFactory csf = new CookieSpecFactory()
        {
            public CookieSpec newInstance(HttpParams params)
            {

                CookieSpec ret =

                new BrowserCompatSpec()
                {

                    @Override
                    public void validate(Cookie cookie, CookieOrigin origin) throws MalformedCookieException
                    {

                        String domain = cookie.getDomain();
                        String host = origin.getHost();
                        KCLog.d("BrowserCompatSpec validate cookie.getDomain: " + domain + " origin.getHost: " + host);

                    }
                };

                return ret;
            }
        };
        httpclient.getCookieSpecs().register("easy", csf);
        httpclient.getParams().setParameter(ClientPNames.COOKIE_POLICY, "easy");

        mInstance.mHttpClient = httpclient;
        mInstance.mHttpClient.setHttpRequestRetryHandler(retryHandler);
    }

    private KCHttpClient()
    {
    }

    public static synchronized DefaultHttpClient getHttpClient()
    {
        if (mInstance == null || mInstance.mHttpClient == null)
        {
            createHttpClient();
        }
        return mInstance.mHttpClient;
    }

    private static String getCustomUA(boolean force)
    {
        if (mCustomUA == null || force)
        {
            mCustomUA = "jsBridge";//UA
        }
        return mCustomUA;
    }

    public static void setUseProxy(boolean useProxy)
    {
        HttpParams params = getHttpClient().getParams();
        HttpProtocolParams.setUserAgent(params, getCustomUA(true));

        if (useProxy)
        {
            try
            {
                params.setParameter(ConnRouteParams.DEFAULT_PROXY, new HttpHost(Proxy.getDefaultHost(), Proxy.getDefaultPort()));
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            params.removeParameter(ConnRouteParams.DEFAULT_PROXY);
        }
    }
}