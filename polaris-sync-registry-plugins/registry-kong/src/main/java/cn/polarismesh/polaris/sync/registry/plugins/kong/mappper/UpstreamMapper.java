package cn.polarismesh.polaris.sync.registry.plugins.kong.mappper;

import cn.polarismesh.polaris.sync.common.database.RecordSupplier;
import cn.polarismesh.polaris.sync.common.utils.CommonUtils;
import cn.polarismesh.polaris.sync.registry.plugins.kong.model.UpstreamObject;

import java.sql.ResultSet;

public class UpstreamMapper implements RecordSupplier<UpstreamObject> {

    @Override
    public String getQueryListSqlTemplate(boolean first) {
        return null;
    }

    @Override
    public String getQueryOneSqlTemplate() {
       return "select * from upstreams where name = ? limit 1;";
    }

    @Override
    public UpstreamObject apply(ResultSet t) throws Exception {

        return new UpstreamObject.Builder().setId(t.getString("id")).setWsId(t.getString("ws_id"))
                .setName("name").setTags(CommonUtils.parseKongTags(t.getString("tags"))).build();
    }

    @Override
    public UpstreamObject merge(UpstreamObject cur, UpstreamObject pre) {
        return null;
    }

}
