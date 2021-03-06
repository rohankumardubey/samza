/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.samza.test.table;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import org.apache.samza.SamzaException;
import org.apache.samza.config.Config;
import org.apache.samza.serializers.IntegerSerde;
import org.apache.samza.serializers.Serde;
import org.apache.samza.serializers.SerdeFactory;


public class TestTableData {
  private static final IntegerSerde INTEGER_SERDE = new IntegerSerde();

  public static class PageView implements Serializable {
    @JsonProperty("pageKey")
    final String pageKey;
    @JsonProperty("memberId")
    final int memberId;

    @JsonProperty("pageKey")
    public String getPageKey() {
      return pageKey;
    }

    @JsonProperty("memberId")
    public int getMemberId() {
      return memberId;
    }

    @JsonCreator
    public PageView(@JsonProperty("pageKey") String pageKey, @JsonProperty("memberId") int memberId) {
      this.pageKey = pageKey;
      this.memberId = memberId;
    }
  }

  public static class Profile implements Serializable {
    @JsonProperty("memberId")
    final int memberId;

    @JsonProperty("company")
    final String company;

    @JsonProperty("memberId")
    public int getMemberId() {
      return memberId;
    }

    @JsonProperty("company")
    public String getCompany() {
      return company;
    }

    @JsonCreator
    public Profile(@JsonProperty("memberId") int memberId, @JsonProperty("company") String company) {
      this.memberId = memberId;
      this.company = company;
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || !(o instanceof Profile)) {
        return false;
      }
      return ((Profile) o).getMemberId() == memberId;
    }

    @Override
    public int hashCode() {
      return memberId;
    }
  }

  public static class EnrichedPageView extends PageView {

    @JsonProperty("company")
    final String company;

    @JsonProperty("company")
    public String getCompany() {
      return company;
    }

    @JsonCreator
    public EnrichedPageView(
        @JsonProperty("pageKey") String pageKey,
        @JsonProperty("memberId") int memberId,
        @JsonProperty("company") String company) {
      super(pageKey, memberId);
      this.company = company;
    }

    @Override
    public int hashCode() {
      return Objects.hash(company, memberId, pageKey);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }

      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      EnrichedPageView that = (EnrichedPageView) o;
      return Objects.equals(company, that.company) && Objects.equals(memberId, that.memberId) && Objects.equals(pageKey,
          that.pageKey);
    }
  }

  public static class PageViewJsonSerdeFactory implements SerdeFactory<PageView> {
    @Override public Serde<PageView> getSerde(String name, Config config) {
      return new PageViewJsonSerde();
    }
  }

  public static class ProfileJsonSerdeFactory implements SerdeFactory<Profile> {
    @Override public Serde<Profile> getSerde(String name, Config config) {
      return new ProfileJsonSerde();
    }
  }

  public static class PageViewJsonSerde implements Serde<PageView> {

    @Override
    public PageView fromBytes(byte[] bytes) {
      try {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(new String(bytes, "UTF-8"), new TypeReference<PageView>() { });
      } catch (Exception e) {
        throw new SamzaException(e);
      }
    }

    @Override
    public byte[] toBytes(PageView pv) {
      try {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(pv).getBytes("UTF-8");
      } catch (Exception e) {
        throw new SamzaException(e);
      }
    }
  }

  public static class ProfileJsonSerde implements Serde<Profile> {

    @Override
    public Profile fromBytes(byte[] bytes) {
      try {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(new String(bytes, "UTF-8"), new TypeReference<Profile>() { });
      } catch (Exception e) {
        throw new SamzaException(e);
      }
    }

    @Override
    public byte[] toBytes(Profile p) {
      try {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(p).getBytes("UTF-8");
      } catch (Exception e) {
        throw new SamzaException(e);
      }
    }
  }

  private static final String[] PAGEKEYS = {"inbox", "home", "search", "pymk", "group", "job"};

  static public PageView[] generatePageViews(int count) {
    Random random = new Random();
    PageView[] pageviews = new PageView[count];
    for (int i = 0; i < count; i++) {
      String pagekey = PAGEKEYS[random.nextInt(PAGEKEYS.length - 1)];
      int memberId = random.nextInt(10);
      pageviews[i] = new PageView(pagekey, memberId);
    }
    return pageviews;
  }

  /**
   * Create page views and spread out page views with the same member id across different partitions.
   * Member ids are spread out like this to make sure that partitionBy operators properly repartition the messages.
   * Member ids are assigned randomly from [0, 10).
   *
   * Example
   * generatePartitionedPageViews(20, 4) will return:
   * 0 -> page views with member ids [0, 5)
   * 1 -> page views with member ids [6, 10)
   * 2 -> page views with member ids [0, 5)
   * 3 -> page views with member ids [6, 10)
   */
  public static Map<Integer, List<PageView>> generatePartitionedPageViews(int numPageViews, int partitionCount) {
    Preconditions.checkArgument(numPageViews % partitionCount == 0, "partitionCount must divide numPageViews evenly");
    int numPerPartition = numPageViews / partitionCount;
    Random random = new Random();
    ImmutableMap.Builder<Integer, List<PageView>> pageViewsBuilder = new ImmutableMap.Builder<>();
    for (int i = 0; i < partitionCount; i++) {
      pageViewsBuilder.put(i, new ArrayList<>());
    }
    Map<Integer, List<PageView>> pageViews = pageViewsBuilder.build();
    for (int i = 0; i < numPageViews; i++) {
      String pagekey = PAGEKEYS[random.nextInt(PAGEKEYS.length - 1)];
      int memberId = i % 10;
      pageViews.get(i / numPerPartition).add(new PageView(pagekey, memberId));
    }
    return pageViews;
  }

  public static PageView[] generatePageViewsWithDistinctKeys(int count) {
    Random random = new Random();
    PageView[] pageviews = new PageView[count];
    for (int i = 0; i < count; i++) {
      String pagekey = PAGEKEYS[random.nextInt(PAGEKEYS.length - 1)];
      pageviews[i] = new PageView(pagekey, i);
    }
    return pageviews;
  }

  private static final String[] COMPANIES = {"MSFT", "LKND", "GOOG", "FB", "AMZN", "CSCO"};

  public static Profile[] generateProfiles(int count) {
    Random random = new Random();
    Profile[] profiles = new Profile[count];
    for (int i = 0; i < count; i++) {
      String company = COMPANIES[random.nextInt(COMPANIES.length - 1)];
      profiles[i] = new Profile(i, company);
    }
    return profiles;
  }

  /**
   * Create profiles and partition them based on the bytes representation of the member id. This uses the bytes
   * representation for partitioning because this needs to use the same partition function as the InMemorySystemProducer
   * (which is used in the test framework) so that table joins can be tested.
   * One profile for each member id in [0, numProfiles) is created.
   */
  public static Map<Integer, List<Profile>> generatePartitionedProfiles(int numProfiles, int partitionCount) {
    Random random = new Random();
    return IntStream.range(0, numProfiles)
        .mapToObj(i -> {
          String company = COMPANIES[random.nextInt(COMPANIES.length - 1)];
          return new Profile(i, company);
        })
        .collect(Collectors.groupingBy(
          profile -> Math.abs(Arrays.hashCode(INTEGER_SERDE.toBytes(profile.getMemberId()))) % partitionCount));
  }
}
