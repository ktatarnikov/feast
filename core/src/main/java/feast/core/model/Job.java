/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2018-2019 The Feast Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package feast.core.model;

import com.google.protobuf.InvalidProtocolBufferException;
import feast.core.job.Runner;
import feast.proto.core.FeatureSetProto;
import feast.proto.core.IngestionJobProto;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.*;
import javax.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/** Contains information about a run job. */
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "jobs")
public class Job extends AbstractTimestampEntity {

  // Internal job name. Generated by feast ingestion upon invocation.
  @Id private String id;

  // External job id, generated by the runner and retrieved by feast.
  // Used internally for job management.
  @Column(name = "ext_id")
  private String extId;

  // Runner type
  @Enumerated(EnumType.STRING)
  @Column(name = "runner")
  private Runner runner;

  // Source id
  @ManyToOne
  @JoinColumn(name = "source_id")
  private Source source;

  // Sink id
  @ManyToOne
  @JoinColumn(name = "store_name")
  private Store store;

  // FeatureSets populated by the job via intermediate FeatureSetJobStatus model
  @OneToMany(mappedBy = "job", cascade = CascadeType.ALL)
  private Set<FeatureSetJobStatus> featureSetJobStatuses = new HashSet<>();

  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 16)
  private JobStatus status;

  public Job() {
    super();
  }

  public boolean hasTerminated() {
    return getStatus().isTerminal();
  }

  public boolean isRunning() {
    return getStatus() == JobStatus.RUNNING;
  }

  public String getSinkName() {
    return store.getName();
  }

  /**
   * Convert a job model to ingestion job proto
   *
   * @return Ingestion Job proto derieved from the given job
   */
  public IngestionJobProto.IngestionJob toProto() throws InvalidProtocolBufferException {

    // convert featuresets of job to protos
    List<FeatureSetProto.FeatureSet> featureSetProtos = new ArrayList<>();
    for (FeatureSetJobStatus featureSet : this.getFeatureSetJobStatuses()) {
      featureSetProtos.add(featureSet.getFeatureSet().toProto());
    }

    // build ingestion job proto with job data
    IngestionJobProto.IngestionJob ingestJob =
        IngestionJobProto.IngestionJob.newBuilder()
            .setId(this.getId())
            .setExternalId(this.getExtId())
            .setStatus(this.getStatus().toProto())
            .setSource(this.getSource().toProto())
            .setStore(this.getStore().toProto())
            .addAllFeatureSets(featureSetProtos)
            .build();

    return ingestJob;
  }
}
