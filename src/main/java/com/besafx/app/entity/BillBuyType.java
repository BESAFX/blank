package com.besafx.app.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;

@Data
@Entity
public class BillBuyType implements Serializable {

    private static final long serialVersionUID = 1L;

    @GenericGenerator(
            name = "billBuyTypeSequenceGenerator",
            strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
            parameters = {
                    @org.hibernate.annotations.Parameter(name = "sequence_name", value = "BILL_BUY_TYPE_SEQUENCE"),
                    @org.hibernate.annotations.Parameter(name = "initial_value", value = "1"),
                    @org.hibernate.annotations.Parameter(name = "increment_size", value = "1")
            }
    )
    @Id
    @GeneratedValue(generator = "billBuyTypeSequenceGenerator")
    private Long id;

    private Integer code;

    private String name;

    @Lob
    @Type(type = "org.hibernate.type.TextType")
    private String note;

    @Temporal(TemporalType.TIMESTAMP)
    private Date lastUpdate;

    @ManyToOne
    @JoinColumn(name = "last_person")
    private Person lastPerson;

    @JsonCreator
    public static BillBuyType Create(String jsonString) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        BillBuyType billBuyType = mapper.readValue(jsonString, BillBuyType.class);
        return billBuyType;
    }
}
