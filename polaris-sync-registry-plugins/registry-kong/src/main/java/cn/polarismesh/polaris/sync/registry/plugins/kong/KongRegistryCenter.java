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

package cn.polarismesh.polaris.sync.registry.plugins.kong;

import cn.polarismesh.polaris.sync.common.database.DatabaseOperator;
import cn.polarismesh.polaris.sync.common.rest.RestOperator;
import cn.polarismesh.polaris.sync.common.rest.RestResponse;
import cn.polarismesh.polaris.sync.common.rest.RestUtils;
import cn.polarismesh.polaris.sync.extension.ResourceType;
import cn.polarismesh.polaris.sync.extension.registry.AbstractRegistryCenter;
import cn.polarismesh.polaris.sync.extension.registry.RegistryInitRequest;
import cn.polarismesh.polaris.sync.extension.registry.Service;
import cn.polarismesh.polaris.sync.model.pb.ModelProto;
import cn.polarismesh.polaris.sync.registry.plugins.kong.mappper.ClusterEventMapper;
import cn.polarismesh.polaris.sync.registry.plugins.kong.mappper.TargetMapper;
import cn.polarismesh.polaris.sync.registry.plugins.kong.mappper.UpstreamMapper;
import cn.polarismesh.polaris.sync.registry.plugins.kong.model.*;
import com.tencent.polaris.client.pb.ResponseProto.DiscoverResponse;
import com.tencent.polaris.client.pb.ServiceProto.Instance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

import org.postgresql.ds.PGPoolingDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Component
public class KongRegistryCenter extends AbstractRegistryCenter {

    private static final Logger LOG = LoggerFactory.getLogger(KongRegistryCenter.class);

    private RegistryInitRequest registryInitRequest;

    private String token;

    private RestOperator restOperator;

    private DatabaseOperator pgOperator;

    @Override
    public String getName() {
        return getType().name();
    }

    @Override
    public ResourceType getType() {
        return ResourceType.KONG;
    }

    @Override
    public void init(RegistryInitRequest registryInitRequest) {
        Assert.hasText(registryInitRequest.getSourceName(), "source registry for kong is empty");
        this.registryInitRequest = registryInitRequest;
        this.token = registryInitRequest.getResourceEndpoint().getAuthorization().getToken();
        restOperator = new RestOperator();

        PGPoolingDataSource source = new PGPoolingDataSource();
        source.setDataSourceName("kong-postgresql");
        //source.setURL("");
        source.setServerName("localhost");
        source.setPortNumber(5432);
        source.setUser("kong");
        source.setPassword("kong");
        source.setCurrentSchema("public");
        source.setStringType("unspecified");
        source.setMaxConnections(100);

        pgOperator = new DatabaseOperator(source);
    }

    @Override
    public void destroy() {

    }

    private void processHealthCheck(RestResponse<?> restResponse) {
        if (restResponse.hasServerError()) {
            serverErrorCount.addAndGet(1);
        }
        totalCount.addAndGet(1);
    }

    @Override
    public DiscoverResponse listInstances(Service service, ModelProto.Group group) {
        throw new UnsupportedOperationException("listInstances is not supported in kong");
    }

    @Override
    public boolean watch(Service service, ResponseListener eventListener) {
        throw new UnsupportedOperationException("watch is not supported in kong");
    }

    @Override
    public void unwatch(Service service) {
        throw new UnsupportedOperationException("unwatch is not supported in kong");
    }

    private boolean resolveAllServices(String address, String nextUrl, List<ServiceObject> services) {
        String servicesUrl;
        if (StringUtils.hasText(nextUrl)) {
            servicesUrl = nextUrl;
        } else {
            servicesUrl = KongEndpointUtils.toServicesUrl(address);
        }
        RestResponse<String> restResponse = restOperator.curlRemoteEndpoint(
                servicesUrl, HttpMethod.GET, RestUtils.getRequestEntity(token, null), String.class);
        processHealthCheck(restResponse);
        if (restResponse.hasServerError()) {
            LOG.error("[Kong] server error to query services {}, reason {}", servicesUrl, restResponse.getException().getMessage());
            return false;
        }
        if (restResponse.hasTextError()) {
            LOG.warn("[Kong] text error to query services {}, code {}, reason {}",
                    servicesUrl, restResponse.getRawStatusCode(), restResponse.getStatusText());
            return false;
        }
        ResponseEntity<String> queryEntity = restResponse.getResponseEntity();
        ServiceObjectList serviceObjectList = RestUtils.unmarshalJsonText(queryEntity.getBody(), ServiceObjectList.class);
        if (null == serviceObjectList) {
            LOG.error("[Kong] invalid response to query services from {}, reason {}", servicesUrl,
                    queryEntity.getBody());
            return false;
        }
        services.addAll(serviceObjectList.getData());
        if (StringUtils.hasText(serviceObjectList.getNext())) {
            nextUrl = replaceHostPort(serviceObjectList.getNext(), address);
            if (!StringUtils.hasText(nextUrl)) {
                return false;
            }
            return resolveAllServices(address, nextUrl, services);
        }
        return true;
    }

    @Override
    public void updateServices(Collection<Service> services) {
    }

    private static final String SCHEME = "http://";

    private String replaceHostPort(String url, String address) {
        if (!url.startsWith(SCHEME)) {
            LOG.error("[Kong] invalid next url {}", url);
            return "";
        }
        String rest = url.substring(SCHEME.length());
        rest = rest.substring(rest.indexOf("/"));
        return SCHEME + address + rest;
    }



    @Override
    public void updateGroups(Service service, Collection<ModelProto.Group> groups) {

    }

    @Override
    public void updateInstances(Service service, ModelProto.Group group, Collection<Instance> instances)  {
        String sourceName = registryInitRequest.getSourceName();
        String sourceType =  registryInitRequest.getSourceType().toString();

        LOG.debug("[Kong] instances to update instances(source {}) group {}, service {}, is {}, ",
                sourceName, group.getName(), service, instances);

        String upstreamName = group.getUpstreamName();
        // 兼容group中没有传upstreamName的旧配置
        if (upstreamName.isEmpty()) {
            upstreamName = ConversionUtils.getUpstreamName(service, group.getName(), sourceName);
        }

        UpstreamMapper mapper = new UpstreamMapper();
        UpstreamObject upstream;
        try {
             upstream = pgOperator.queryOne(mapper.getQueryOneSqlTemplate(), new Object[]{upstreamName}, mapper);
        } catch (Exception e) {
            LOG.warn("[Kong] fail to get upstream: {}, error:{} .", upstreamName, e.toString());
            return;
        }

        if (Objects.isNull(upstream)) {
            // 同步组件配置没及时更新，可能会出现此情况
            LOG.warn("[Kong] upstream: {} is not found.", upstreamName);
            return;
        }

        // upstream配置的服务来源类型，是否与upstream一致，如果不一致，不需要执行下面的同步流程
        boolean matchType = false;
        if ( upstream.getTags() != null) {
            for (String tag : upstream.getTags()) {
                if (tag.equalsIgnoreCase(sourceType)) {
                    matchType = true;
                    break;
                }
            }
        }

        if (!matchType) {
            LOG.info("[Kong] upstream: {} sourceType is {}, not the same as {}", upstreamName,
                    sourceType, String.join(",", upstream.getTags()));
            return;
        }

        TargetMapper tMapper = new TargetMapper();
        List<TargetObject> targets;
        try {
            targets = pgOperator.queryList(tMapper.getQueryListSqlTemplate(true), new Object[]{upstream.getId()},tMapper );
        } catch (Exception e) {
            // todo 异常处理
            throw new RuntimeException(e);
        }

        TargetObjectList targetObjectList = new TargetObjectList();
        targetObjectList.setData(targets);
        Map<String, TargetObject> targetObjectMap = ConversionUtils.parseTargetObjects(targetObjectList);

        boolean override = false;
        HashMap<String, Instance> healthyInstances = new HashMap<>();
        for (Instance instance : instances) {
            boolean healthy = instance.getHealthy().getValue();
            boolean isolated = instance.getIsolate().getValue();
            if (!healthy || isolated) {
                continue;
            }


            String address = String.format("%s:%d", instance.getHost().getValue(), instance.getPort().getValue());
            // 防止由于target相同的instance推送过来，导致数据库内容异常。有可能TCP/UDP同时存在
            if (healthyInstances.get(address) == null) {
                healthyInstances.put(address, instance);
            }

            TargetObject targetObject = targetObjectMap.get(address);
            // 当前target 不能存在 或权重发生变化，需要全量更新
            if (Objects.isNull(targetObject) || targetObject.getWeight() != instance.getWeight().getValue()) {
                override = true;
            }
        }

        // 推空保护
        if (healthyInstances.isEmpty())  {
            LOG.info("[Kong] upstream: {} new targets is empty, do nothing.", upstreamName);
            return;
        }

        if (!override) {
            LOG.info("[Kong] upstream: {} has not changed, do nothing.", upstreamName);
        } else {

            ClusterEventObject clusterEvent = new ClusterEventObject.Builder().setChannel("balancer:targets")
                    .setData(String.format("update:%s", upstream.getId())).build();

            ClusterEventMapper eventMapper = new ClusterEventMapper();
            // 在一个事务内完成
            try {
                Connection connection = pgOperator.getConnection();
                connection.setAutoCommit(false);

                PreparedStatement deleteStatement = connection.prepareStatement(tMapper.getDeleteSqlTemplate());
                deleteStatement.setObject(1, upstream.getId());

                PreparedStatement insertStatement = connection.prepareStatement(tMapper.getInsertOneSqlTemplate());
                for (Map.Entry<String, Instance> instance : healthyInstances.entrySet() ) {
                    insertStatement.setObject(1, UUID.randomUUID().toString());
                    insertStatement.setObject(2, upstream.getId());
                    // key就是target
                    insertStatement.setObject(3, instance.getKey());
                    insertStatement.setObject(4, instance.getValue().getWeight().getValue());
                    insertStatement.setObject(5, upstream.getWsId());
                    insertStatement.addBatch();
                }

                PreparedStatement eventStatement = connection.prepareStatement(eventMapper.getInsertOneSqlTemplate());
                eventStatement.setObject(1, clusterEvent.getId());
                eventStatement.setObject(2, clusterEvent.getNodeId());
                eventStatement.setObject(3, clusterEvent.getAt());
                eventStatement.setObject(4, clusterEvent.getExpireAt());
                eventStatement.setObject(5, clusterEvent.getChannel());
                eventStatement.setObject(6, clusterEvent.getData());

                deleteStatement.executeUpdate();
                insertStatement.executeBatch();
                eventStatement.executeUpdate();
                connection.commit();

                LOG.info("[Kong] upstream: {} has changed, override it.", upstreamName);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }


        }
    }

    private <T> void commonCreateOrUpdateRequest(
            String name, String serviceUrl, HttpMethod method, T serviceObject, String operation) {
        String jsonText = "";
        if (null != serviceObject) {
            jsonText = RestUtils.marshalJsonText(serviceObject);
        }
        RestResponse<String> restResponse = restOperator.curlRemoteEndpoint(
                serviceUrl, method, RestUtils.getRequestEntity(token, jsonText), String.class);
        processHealthCheck(restResponse);
        if (restResponse.hasServerError()) {
            LOG.error("[Kong] server error to {} {} to {}, method {}, request {}, reason {}",
                    operation, name, serviceUrl, method.name(), jsonText, restResponse.getException().getMessage());
            return;
        }
        if (restResponse.hasTextError()) {
            LOG.warn("[Kong] text error to {} {} to {}, method {}, request {}, code {}, reason {}",
                    operation, name, serviceUrl, method.name(), jsonText, restResponse.getRawStatusCode(),
                    restResponse.getStatusText());
            return;
        }
        LOG.info("[Kong] success to {} {} to {}, method {}, request {}", operation, name, serviceUrl, method.name(),
                jsonText);
    }

    private void processServiceRequest(
            String serviceUrl, HttpMethod method, ServiceObject serviceObject, String operation) {
        commonCreateOrUpdateRequest("service", serviceUrl, method, serviceObject, operation);
    }

    private void processUpstreamRequest(
            String upstreamUrl, HttpMethod method, UpstreamObject upstreamObject, String operation) {
        commonCreateOrUpdateRequest("upstream", upstreamUrl, method, upstreamObject, operation);
    }

    private void processTargetRequest(String targetUrl, HttpMethod method, TargetObject targetObject,
            String operation) {
        commonCreateOrUpdateRequest("target", targetUrl, method, targetObject, operation);
    }

}
