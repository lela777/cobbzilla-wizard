package org.cobbzilla.wizard.docstore.mongo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.jmkgreen.morphia.annotations.Id;
import com.github.jmkgreen.morphia.annotations.Indexed;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.cobbzilla.wizard.model.Identifiable;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import java.util.UUID;

import static org.cobbzilla.wizard.model.BasicConstraintConstants.ERR_UUID_LENGTH;
import static org.cobbzilla.wizard.model.BasicConstraintConstants.UUID_MAXLEN;

public class MongoDocBase implements Identifiable {

    public static final String UUID = "uuid";

    @Id @JsonIgnore @Getter @Setter
    private ObjectId id;

    @Getter @Setter @Indexed
    @Size(max=UUID_MAXLEN, message=ERR_UUID_LENGTH)
    private String uuid;

    @Override
    public void beforeCreate() {
        if (uuid != null) return; // caller is supplying it to link to something else
        uuid = java.util.UUID.randomUUID().toString();
    }

    @NotNull @Setter
    private long ctime = System.currentTimeMillis();
    @JsonIgnore public long getCtime () { return ctime; }

}
