/*
 * Tencent is pleased to support the open source community by making Polaris available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package cn.polarismesh.polaris.sync.registry.plugins.kong.model;

import cn.polarismesh.polaris.sync.common.database.RecordSupplier;

import java.util.List;
import java.util.Objects;

public class UpstreamObject {

    private String id;

    private String name;

    public String getWsId() {
        return wsId;
    }

    private String wsId;

    private List<String> tags;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    @Override
    public String toString() {
        return "Upstream{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", tags=" + tags +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UpstreamObject)) {
            return false;
        }
        UpstreamObject that = (UpstreamObject) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(tags, that.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, tags);
    }

    public UpstreamObject() {

    }
    private UpstreamObject(Builder builder) {
        this.tags = builder.tags;
        this.id = builder.id;
        this.name = builder.name;
        this.wsId = builder.wsId;

    }
    public static class Builder{
        private String id;

        private String name;

        private String wsId;

        private List<String> tags;

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return  this;
        }

        public Builder setTags(List<String> tags) {
            this.tags = tags;
            return this;
        }

        public Builder setWsId(String wsId) {
            this.wsId = wsId;
            return this;
        }

        public UpstreamObject build() {
            return new UpstreamObject(this);
        }
    }
}
