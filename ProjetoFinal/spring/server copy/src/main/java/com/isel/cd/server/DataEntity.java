package com.isel.cd.server;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "server_data")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class DataEntity implements Serializable{
    @Id
    private String key;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private Data data;

    public DataEntity(String key) {
        this.key = key;
    }

    @lombok.Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Data implements Serializable {
        private String data;
    }
}
