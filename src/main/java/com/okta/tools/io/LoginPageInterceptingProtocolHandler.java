/*
 * Copyright 2019 Okta
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.okta.tools.io;

import com.okta.tools.OktaAwsCliEnvironment;



import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.logging.Logger;

final class LoginPageInterceptingProtocolHandler extends URLStreamHandler {
    private static final Logger LOGGER = Logger.getLogger(LoginPageInterceptingProtocolHandler.class.getName());
    private final OktaAwsCliEnvironment environment;
    private final BiFunction<URL, URLConnection, URLConnection> filteringUrlConnectionFactory;

    LoginPageInterceptingProtocolHandler(OktaAwsCliEnvironment environment, BiFunction<URL, URLConnection, URLConnection> filteringUrlConnectionFactory) {
        this.environment = environment;
        this.filteringUrlConnectionFactory = filteringUrlConnectionFactory;
    }


    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        return this.openConnection(url,(Proxy)null);
    }

    private URLConnection handleOpenConnection(URL url, URLConnection urlConnection) throws IOException{
        URI oktaAwsAppUri = URI.create(environment.oktaAwsAppUrl);
        List<String> domainsToIntercept = Arrays.asList(
                environment.oktaOrg,
                oktaAwsAppUri.getHost()
        );
        List<String> requestPathsToIntercept = Arrays.asList(
                oktaAwsAppUri.getPath(),
                "/login/login.htm",
                "/auth/services/devicefingerprint"
        );
        if (domainsToIntercept.contains(url.getHost()) &&
                requestPathsToIntercept.contains(url.getPath())
        ) {
            LOGGER.finest(() -> String.format("[%s] Using filtering URLConnection", url));
            return filteringUrlConnectionFactory.apply(url, urlConnection);
        } else {
            LOGGER.finest(() -> String.format("[%s] Using unmodified URLConnection", url));
            return urlConnection;
        }
    }

    @Override
    protected URLConnection openConnection(URL url, Proxy proxy) throws IOException {
        URLConnection urlConnection = super.openConnection(url, proxy);
        //URLConnection urlConnection = new HttpURLConnection(url, url.getHost(), url.getPort());
        // default handler
        //URLStreamHandler defaultHandler = new sun.net.www.protocol.https.Handler();
        //URLConnection urlConnection = new HttpURLConnection(url, proxy, this);
        //URLConnection urlConnection = HttpConnectionFactory.GetConnection(url,proxy,this);
        //URLConnection = new HttpsURLConnection(url,proxy, this)
        return handleOpenConnection(url, urlConnection);
    }
}
