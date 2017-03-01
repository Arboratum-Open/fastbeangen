package com.arboratum.beangen.database;

import com.arboratum.beangen.BaseBuilders;
import com.arboratum.beangen.Generator;
import com.arboratum.beangen.util.RandomSequence;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by gpicron on 13/02/2017.
 */
public class DataSetBuilderTest {
    public static class Pojo {
        private long id;
        private String name;

        public Pojo() {
        }

        public long getId() {
            return this.id;
        }

        public String getName() {
            return this.name;
        }

        public void setId(long id) {
            this.id = id;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof Pojo)) return false;
            final Pojo other = (Pojo) o;
            if (!other.canEqual((Object) this)) return false;
            if (this.getId() != other.getId()) return false;
            final Object this$name = this.getName();
            final Object other$name = other.getName();
            if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
            return true;
        }

        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final long $id = this.getId();
            result = result * PRIME + (int) ($id >>> 32 ^ $id);
            final Object $name = this.getName();
            result = result * PRIME + ($name == null ? 43 : $name.hashCode());
            return result;
        }

        protected boolean canEqual(Object other) {
            return other instanceof Pojo;
        }

        public String toString() {
            return "com.arboratum.beangen.database.DataSetBuilderTest.Pojo(id=" + this.getId() + ", name=" + this.getName() + ")";
        }
    }

    @Test
    public void initiallyGeneratedBy() throws Exception {
        Generator<Pojo> generator = BaseBuilders.aBean(Pojo.class).withDefaultFieldPopulators().build();
        Generator<Integer> update = BaseBuilders.aInteger().uniform(0, 5).build();

        UpdateGenerator<Pojo> updateGenerator = new UpdateGenerator<Pojo>() {

            @Override
            public UpdateOf<Pojo> generate(Pojo previousValue, RandomSequence randomSequence) {
                return pojo -> {
                    pojo.name += update.generate(randomSequence).intValue();

                    return pojo;
                };
            }
        };


        final DataSet<Pojo> build = new DataSetBuilder<Pojo>()
                .of(10000000)
                .initiallyGeneratedBy(5, 4, 1)
                .thenUpdatedWith(5, 4, 1)
                .withEntryGenerator(generator)
                .withUpdateGenerator(updateGenerator)
                .build();

        Assert.assertEquals(10000000, build.getSize());
        Assert.assertEquals(12505555, build.getLastIndex());

    }

}