package org.cobbzilla.wizard.docstore.mongo;

import com.github.jmkgreen.morphia.annotations.Id;
import org.bson.types.ObjectId;
import org.codehaus.jackson.annotate.JsonIgnore;

import javax.validation.constraints.NotNull;

public class MongoDocBase {

    @Id
    @JsonIgnore
    private ObjectId id;
    public ObjectId getId() { return id; }

    @NotNull
    @JsonIgnore
    private long ctime = System.currentTimeMillis();
    public long getCtime() { return ctime; }
    public void setCtime(long ctime) { this.ctime = ctime; }

}
