package com.smartcloset.weather.infrastructure.kma;

import java.io.IOException;
import java.net.URI;

interface KmaHttpTransport {

    KmaHttpResponse get(URI uri) throws IOException, InterruptedException;
}
