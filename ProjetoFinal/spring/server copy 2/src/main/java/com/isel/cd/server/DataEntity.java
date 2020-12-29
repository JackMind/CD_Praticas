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

    public DataEntity(DataEntity create) {
        this.key = create.getKey();
        this.data = create.getData();
    }

    public DataEntity(DataEntity.DataDto create) {
        this.key = create.getKey();
        this.data = new Data(create.getData());
    }




    @lombok.Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Data implements Serializable {
        private String data;
    }

    @lombok.Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DataDto implements Serializable {
        private String key;
        private String data;

        public DataDto(DataEntity dataEntity){
            this.key = dataEntity.getKey();
            this.data = dataEntity.getData().getData();
        }
    }
}
