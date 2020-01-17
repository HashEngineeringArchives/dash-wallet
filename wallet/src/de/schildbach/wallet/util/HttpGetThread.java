/*
 * Copyright 2013-2015 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.schildbach.wallet.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;

import javax.annotation.Nullable;

import de.schildbach.wallet.Constants;
import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @author Andreas Schildbach
 */
public abstract class HttpGetThread extends Thread {
    private final HttpUrl url;
    @Nullable
    private final String userAgent;

    private static final Logger log = LoggerFactory.getLogger(HttpGetThread.class);

    public HttpGetThread(final HttpUrl url, @Nullable final String userAgent) {
        this.url = url;
        this.userAgent = userAgent;
    }

    @Override
    public void run() {
        log.debug("querying \"{}\"...", url);

        final Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .header("Accept-Charset", "utf-8");
        if (userAgent != null)
            requestBuilder.header("User-Agent", userAgent);

        final Call call = Constants.HTTP_CLIENT.newCall(requestBuilder.build());
        try {
            final Response response = call.execute();
            if (response.isSuccessful()) {
                final long serverTime = response.headers().getDate("Date").getTime();
                final BufferedReader reader = new BufferedReader(response.body().charStream());
                final String line = reader.readLine().trim();
                reader.close();

                handleLine(line, serverTime);
            }
        } catch (final Exception x) {
            handleException(x);
        }
    }

    protected abstract void handleLine(String line, long serverTime);

    protected abstract void handleException(Exception x);
}
