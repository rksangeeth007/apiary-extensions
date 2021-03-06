/**
 * Copyright (C) 2018-2019 Expedia, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * Contains code from the Apache Hive project, specifically:
 * copied from https://github.com/apache/hive/blob/branch-2.3/common/src/test/org/apache/hadoop/hive/common/metrics/metrics2/TestCodahaleMetrics.java
 * removed testFileReporting(), added some code to set System Environment Variables
 */
package com.expediagroup.apiary.extensions.metastore.metrics;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.apache.hadoop.hive.common.metrics.common.MetricsFactory;
import org.apache.hadoop.hive.common.metrics.common.MetricsVariable;
import org.apache.hadoop.hive.conf.HiveConf;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

/**
 * Unit test for new Metrics subsystem.
 */
public class CodahaleMetricsTest {

  @Rule
  public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

  public static MetricRegistry metricRegistry;

  @Before
  public void before() throws Exception {
    environmentVariables.set("CLOUDWATCH_NAMESPACE", "some-cloud-watch-namespace");
    environmentVariables.set("ECS_TASK_ID", "some-task-id");
    environmentVariables.set("AWS_REGION", "us-west-2");

    HiveConf conf = new HiveConf();

    conf.set(CommonConfigurationKeysPublic.FS_DEFAULT_NAME_KEY, "local");
    conf.setVar(HiveConf.ConfVars.HIVE_METRICS_CLASS, CodahaleMetrics.class.getCanonicalName());

    MetricsFactory.init(conf);
    metricRegistry = ((CodahaleMetrics) MetricsFactory.getInstance()).getMetricRegistry();
  }

  @After
  public void after() throws Exception {
    MetricsFactory.close();
  }

  @Test
  public void testScope() throws Exception {
    int runs = 5;
    for (int i = 0; i < runs; i++) {
      MetricsFactory.getInstance().startStoredScope("method1");
      MetricsFactory.getInstance().endStoredScope("method1");
    }

    Timer timer = metricRegistry.getTimers().get("method1");
    Assert.assertEquals(5, timer.getCount());
    Assert.assertTrue(timer.getMeanRate() > 0);
  }


  @Test
  public void testCount() throws Exception {
    int runs = 5;
    for (int i = 0; i < runs; i++) {
      MetricsFactory.getInstance().incrementCounter("count1");
    }
    Counter counter = metricRegistry.getCounters().get("count1");
    Assert.assertEquals(5L, counter.getCount());
  }

  @Test
  public void testConcurrency() throws Exception {
    int threads = 4;
    ExecutorService executorService = Executors.newFixedThreadPool(threads);
    for (int i=0; i< threads; i++) {
      executorService.submit(new Callable<Void>() {
        @Override
        public Void call() throws Exception {
          MetricsFactory.getInstance().startStoredScope("method2");
          MetricsFactory.getInstance().endStoredScope("method2");
          return null;
        }
      });
    }
    executorService.shutdown();
    assertTrue(executorService.awaitTermination(10000, TimeUnit.MILLISECONDS));
    Timer timer = metricRegistry.getTimers().get("method2");
    Assert.assertEquals(4, timer.getCount());
    Assert.assertTrue(timer.getMeanRate() > 0);
  }

  class TestMetricsVariable implements MetricsVariable {
    private int gaugeVal;

    @Override
    public Object getValue() {
      return gaugeVal;
    }
    public void setValue(int gaugeVal) {
      this.gaugeVal = gaugeVal;
    }
  };

  @Test
  public void testGauge() throws Exception {
    TestMetricsVariable testVar = new TestMetricsVariable();
    testVar.setValue(20);

    MetricsFactory.getInstance().addGauge("gauge1", testVar);
    String json = ((CodahaleMetrics) MetricsFactory.getInstance()).dumpJson();
    MetricsTestUtils.verifyMetricsJson(json, MetricsTestUtils.GAUGE, "gauge1", testVar.getValue());


    testVar.setValue(40);
    json = ((CodahaleMetrics) MetricsFactory.getInstance()).dumpJson();
    MetricsTestUtils.verifyMetricsJson(json, MetricsTestUtils.GAUGE, "gauge1", testVar.getValue());
  }

  @Test
  public void testMeter() throws Exception {

    String json = ((CodahaleMetrics) MetricsFactory.getInstance()).dumpJson();
    MetricsTestUtils.verifyMetricsJson(json, MetricsTestUtils.METER, "meter", "");

    MetricsFactory.getInstance().markMeter("meter");
    json = ((CodahaleMetrics) MetricsFactory.getInstance()).dumpJson();
    MetricsTestUtils.verifyMetricsJson(json, MetricsTestUtils.METER, "meter", "1");

    MetricsFactory.getInstance().markMeter("meter");
    json = ((CodahaleMetrics) MetricsFactory.getInstance()).dumpJson();
    MetricsTestUtils.verifyMetricsJson(json, MetricsTestUtils.METER, "meter", "2");

  }
}
