/*
 * Copyright (c) 2014, Oracle America, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of Oracle nor the names of its contributors may be used
 *    to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package br.ufsc.inf.lapesd;

import br.ufsc.inf.lapesd.linkedator.Linkedator;
import br.ufsc.inf.lapesd.linkedator.ModelBasedLinkedator;
import br.ufsc.inf.lapesd.linkedator.SemanticMicroserviceDescription;
import br.ufsc.inf.lapesd.linkedator.links.LinkVerifier;
import br.ufsc.inf.lapesd.linkedator.links.NullLinkVerifier;
import com.google.gson.Gson;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.openjdk.jmh.annotations.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@BenchmarkMode({Mode.AverageTime, Mode.SingleShotTime})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
public class LinkCreationBenchmark {

    @Benchmark
    public Model inferredLinkCreation(InferredLinkConfig config) {
        config.linkedator.createLinks(config.input, config.linkVerifier);
        return config.input;
    }

    @State(Scope.Thread)
    public static class InferredLinkConfig {
        Linkedator linkedator;
        Model input;
        LinkVerifier linkVerifier;

        @Setup(Level.Invocation)
        public void setup() throws IOException {
            linkVerifier = new NullLinkVerifier();

            linkedator = new ModelBasedLinkedator();
            Model onto = ModelFactory.createDefaultModel();
            try (InputStream in = getClass().getResourceAsStream("/cities/domainOntology.ttl")) {
                RDFDataMgr.read(onto, in, Lang.TURTLE);
            }
            linkedator.updateOntologies(onto);

            SemanticMicroserviceDescription smd;
            smd = new Gson().fromJson(new InputStreamReader(getClass().getResourceAsStream("/cities/microserviceOfCities.json"), StandardCharsets.UTF_8), SemanticMicroserviceDescription.class);
            smd.setIpAddress("192.168.10.1");
            linkedator.register(smd);

            smd = new Gson().fromJson(new InputStreamReader(getClass().getResourceAsStream("/cities/microserviceOfStates.json"), StandardCharsets.UTF_8), SemanticMicroserviceDescription.class);
            smd.setIpAddress("192.168.10.2");
            linkedator.register(smd);

            input = ModelFactory.createDefaultModel();
            try (InputStream in = getClass().getResourceAsStream("/cities/city.ttl")) {
                RDFDataMgr.read(input, in, Lang.TURTLE);
            }
        }
    }
}
