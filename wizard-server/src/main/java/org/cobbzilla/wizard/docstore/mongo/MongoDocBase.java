package org.cobbzilla.wizard.docstore.mongo;

import com.github.jmkgreen.morphia.annotations.Id;
import com.github.jmkgreen.morphia.annotations.Indexed;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.cobbzilla.wizard.model.BasicConstraintConstants;
import org.codehaus.jackson.annotate.JsonIgnore;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class MongoDocBase {

    public static final String UUID = "uuid";

    @Id @JsonIgnore @Getter @Setter
    private ObjectId id;

    @Getter @Setter @Indexed
    @Size(max= BasicConstraintConstants.UUID_MAXLEN)
    private String uuid;

    @NotNull @JsonIgnore @Getter @Setter
    private long ctime = System.currentTimeMillis();

}
