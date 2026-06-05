package com.smartcloset.weather.infrastructure.kma;

import java.io.IOException;
import java.net.URI;

/**
 * KMA HTTP 호출 방식을 client parsing 로직에서 분리하는 package-private transport boundary다.
 */
interface KmaHttpTransport {

    /**
     * 지정한 KMA URI를 호출하고 HTTP status와 body를 그대로 반환한다.
     */
    KmaHttpResponse get(URI uri) throws IOException, InterruptedException;
}
