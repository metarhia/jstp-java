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
package com.metarhia.jstp.benchmarks;

import com.metarhia.jstp.core.JSParser;
import com.metarhia.jstp.core.JSParsingException;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class JSParserBenchmark {

    public interface Worker {
        void work();
    }

    Worker packerParser;

    @Setup
    public void setup(final Blackhole bh) {
        packerParser = new Worker() {
            String inputData = "{\n" +
                "    login: {\n" +
                "      control: 'screen',\n" +
                "      controls: {\n" +
                "        login: {\n" +
                "          control: 'edit',\n" +
                "          filter: 'login',\n" +
                "          top: 10, left: 10, right: 10,\n" +
                "          height: 10,\n" +
                "          label: 'login'\n" +
                "        },\n" +
                "        password: {\n" +
                "          control: 'edit',\n" +
                "          mode: 'password',\n" +
                "          top: 25, left: 10, right: 10,\n" +
                "          height: 10,\n" +
                "          label: 'password'\n" +
                "        },\n" +
                "        cancel: {\n" +
                "          control: 'button',\n" +
                "          top: 40, right: 70,\n" +
                "          width: 25, height: 10,\n" +
                "          text: 'Cancel'\n" +
                "        },\n" +
                "        signin: {\n" +
                "          control: 'button',\n" +
                "          top: 40, right: 10,\n" +
                "          width: 25, height: 10,\n" +
                "          text: 'Sign in'\n" +
                "        },\n" +
                "        social: {\n" +
                "          control: 'panel',\n" +
                "          top: 55, botton: 10, left: 10, right: 10,\n" +
                "          controls: {\n" +
                "            googlePlus: {\n" +
                "              control: 'button',\n" +
                "              top: 0, left: 0,\n" +
                "              height: 10, width: 10,\n" +
                "              image: 'googlePlus'\n" +
                "            },\n" +
                "            facebook: {\n" +
                "              control: 'button',\n" +
                "              top: 0, left: 10,\n" +
                "              height: 10, width: 10,\n" +
                "              image: 'facebook'\n" +
                "            },\n" +
                "            vk: {\n" +
                "              control: 'button',\n" +
                "              top: 0, left: 10,\n" +
                "              height: 10, width: 10,\n" +
                "              image: 'vk'\n" +
                "            },\n" +
                "            twitter: {\n" +
                "              control: 'button',\n" +
                "              top: 0, left: 20,\n" +
                "              height: 10, width: 10,\n" +
                "              image: 'twitter'\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    main: {\n" +
                "      control: 'screen',\n" +
                "      controls: {\n" +
                "        message: {\n" +
                "          control: 'label',\n" +
                "          top: 10, left: 10, right: 10,\n" +
                "          height: 10,\n" +
                "          text: 'You are logged in'\n" +
                "        }\n" +
                "    }\n" +
                "   }\n" +
                "}";

            JSParser parser = new JSParser();

            @Override
            public void work() {
                parser.setInput(inputData);
                try {
                    bh.consume(parser.parse());
                } catch (JSParsingException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    @Benchmark
    @BenchmarkMode({Mode.Throughput, Mode.AverageTime})
    @Warmup(iterations = 5)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Fork(3)
    public void testPacket() {
        packerParser.work();
    }

}
