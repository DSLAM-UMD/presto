/*
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
package com.facebook.presto.benchmark;

import com.facebook.presto.benchmark.framework.BenchmarkQuery;
import com.facebook.presto.benchmark.framework.BenchmarkSuite;
import com.facebook.presto.benchmark.framework.BenchmarkSuiteInfo;
import com.facebook.presto.benchmark.framework.ConcurrentExecutionPhase;
import com.facebook.presto.benchmark.framework.PhaseSpecification;
import com.facebook.presto.benchmark.framework.StreamExecutionPhase;
import com.facebook.presto.benchmark.source.BenchmarkSuiteDao;
import com.facebook.presto.testing.mysql.TestingMySqlServer;
import com.google.common.collect.ImmutableList;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.facebook.presto.benchmark.framework.PhaseSpecification.ExecutionStrategy.CONCURRENT;
import static com.facebook.presto.benchmark.framework.PhaseSpecification.ExecutionStrategy.STREAM;

public class BenchmarkTestUtil
{
    public static final String CATALOG = "benchmark";
    public static final String SCHEMA = "default";
    public static final String XDB = "presto";

    private BenchmarkTestUtil()
    {
    }

    public static TestingMySqlServer setupMySql()
            throws Exception
    {
        TestingMySqlServer mySqlServer = new TestingMySqlServer("testuser", "testpass", ImmutableList.of(XDB));
        Handle handle = getJdbi(mySqlServer).open();
        BenchmarkSuiteDao benchmarkDao = handle.attach(BenchmarkSuiteDao.class);
        benchmarkDao.createBenchmarkSuitesTable("benchmark_suites");
        benchmarkDao.createBenchmarkQueriesTable("benchmark_queries");
        return mySqlServer;
    }

    public static Jdbi getJdbi(TestingMySqlServer mySqlServer)
    {
        return Jdbi.create(mySqlServer.getJdbcUrl(XDB)).installPlugin(new SqlObjectPlugin());
    }

    public static void insertBenchmarkQuery(Handle handle, String querySet, String name, String query)
    {
        handle.execute(
                "INSERT INTO benchmark_queries(\n" +
                        "    `query_set`, `name`, `catalog`, `schema`, `query`)\n" +
                        "SELECT\n" +
                        "    ?,\n" +
                        "    ?,\n" +
                        "    'benchmark',\n" +
                        "    'default',\n" +
                        "    ?\n",
                querySet,
                name,
                query);
    }

    public static void insertBenchmarkSuite(Handle handle, String suite, String querySet, String phases, String sessionProperties)
    {
        handle.execute(
                "INSERT INTO benchmark_suites(\n" +
                        "    `suite`, `query_set`, `phases`, `session_properties`, `created_by`)\n" +
                        "SELECT\n" +
                        "    ?,\n" +
                        "    ?,\n" +
                        "    ?,\n" +
                        "    ?,\n" +
                        "    'benchmark'\n",
                suite,
                querySet,
                phases,
                sessionProperties);
    }

    public static List<PhaseSpecification> getBenchmarkSuitePhases()
    {
        List<List<String>> streams = ImmutableList.of(ImmutableList.of("Q1", "Q2"), ImmutableList.of("Q2", "Q3"));
        PhaseSpecification streamExecutionPhase = new StreamExecutionPhase("Phase-1", STREAM, streams);

        List<String> queries = ImmutableList.of("Q1", "Q2", "Q3");
        PhaseSpecification concurrentExecutionPhase = new ConcurrentExecutionPhase("Phase-2", CONCURRENT, queries);

        return ImmutableList.of(streamExecutionPhase, concurrentExecutionPhase);
    }

    public static Map<String, String> getBenchmarkSuiteSessionProperties()
    {
        Map<String, String> sessionProperties = new HashMap();
        sessionProperties.put("max", "5");
        return sessionProperties;
    }

    public static BenchmarkSuite getBenchmarkSuiteObject(String suite, String querySet)
    {
        BenchmarkQuery benchmarkQuery1 = new BenchmarkQuery(querySet, "Q1", "SELECT 1", CATALOG, SCHEMA);
        BenchmarkQuery benchmarkQuery2 = new BenchmarkQuery(querySet, "Q2", "SELECT 2", CATALOG, SCHEMA);
        BenchmarkQuery benchmarkQuery3 = new BenchmarkQuery(querySet, "Q3", "SELECT 3", CATALOG, SCHEMA);

        return new BenchmarkSuite(new BenchmarkSuiteInfo(suite, querySet, getBenchmarkSuitePhases(), getBenchmarkSuiteSessionProperties()),
                ImmutableList.of(benchmarkQuery1, benchmarkQuery2, benchmarkQuery3));
    }
}
