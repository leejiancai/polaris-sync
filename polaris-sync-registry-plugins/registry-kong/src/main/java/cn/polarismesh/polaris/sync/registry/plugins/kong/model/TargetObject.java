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

import java.lang.annotation.Target;
import java.util.Objects;

public class TargetObject {

    private String id;

    private String target;

    private int weight;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TargetObject)) {
            return false;
        }
        TargetObject that = (TargetObject) o;
        return weight == that.weight &&
                Objects.equals(id, that.id) &&
                Objects.equals(target, that.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, target, weight);
    }

    @Override
    public String toString() {
        return String.format("[%s:%d]", target, weight);
    }

    public TargetObject() {

    }

    public TargetObject(Builder builder) {
        this.id = builder.id;
        this.target = builder.target;
        this.weight = builder.weight;
    }
    public static class Builder {
        private String id;

        private String target;

        private int weight;

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Builder setTarget(String target) {
            this.target = target;
            return this;
        }

        public Builder setWeight(int weight) {
            this.weight = weight;
            return this;
        }

        public TargetObject build() {
            return new TargetObject(this);
        }
    }
}
