package org.cobbzilla.wizard.docstore.mongo;

import com.github.jmkgreen.morphia.annotations.Id;
import com.github.jmkgreen.morphia.annotations.Indexed;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.codehaus.jackson.annotate.JsonIgnore;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import static org.cobbzilla.wizard.model.BasicConstraintConstants.ERR_UUID_LENGTH;
import static org.cobbzilla.wizard.model.BasicConstraintConstants.UUID_MAXLEN;

public class MongoDocBase {

    public static final String UUID = "uuid";

    @Id @JsonIgnore @Getter @Setter
    private ObjectId id;

    @Getter @Setter @Indexed
    @Size(max=UUID_MAXLEN, message=ERR_UUID_LENGTH)
    private String uuid;

    @NotNull @JsonIgnore @Getter @Setter
    private long ctime = System.currentTimeMillis();

}
