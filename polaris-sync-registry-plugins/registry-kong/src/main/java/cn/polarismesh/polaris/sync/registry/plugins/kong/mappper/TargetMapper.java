package cn.polarismesh.polaris.sync.registry.plugins.kong.mappper;

import cn.polarismesh.polaris.sync.common.database.RecordSupplier;
import cn.polarismesh.polaris.sync.registry.plugins.kong.model.TargetObject;

import java.sql.ResultSet;

public class TargetMapper implements RecordSupplier<TargetObject> {

    @Override
    public String getQueryListSqlTemplate(boolean first) {
        return "select * from targets where upstream_id = ?;";
    }

    @Override
    public String getQueryOneSqlTemplate() {
        return null;
    }

    public String getInsertOneSqlTemplate() {
        return "insert into targets(id,created_at,upstream_id,target,weight,ws_id) values(?,?,?,?,?,?);";
    }

    public String getDeleteSqlTemplate() {
        return "delete from targets where upstream_id = ?;";
    }

    @Override
    public TargetObject apply(ResultSet t) throws Exception {

        return new TargetObject.Builder().setId(t.getString("id"))
                .setTarget(t.getString("target")).setWeight(t.getInt("weight")).build();
    }

    @Override
    public TargetObject merge(TargetObject cur, TargetObject pre) {
        return null;
    }
}
