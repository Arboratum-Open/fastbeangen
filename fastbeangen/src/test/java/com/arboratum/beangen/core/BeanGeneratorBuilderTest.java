package com.arboratum.beangen.core;

import com.arboratum.beangen.BaseBuilders;
import com.arboratum.beangen.Generator;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

/**
 * Created by gpicron on 10/08/2016.
 */
public class BeanGeneratorBuilderTest {

    public static class TestBean {
        private long id;
        private String name;

        public TestBean() {
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
            if (!(o instanceof TestBean)) return false;
            final TestBean other = (TestBean) o;
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
            return other instanceof TestBean;
        }

        public String toString() {
            return "com.arboratum.beangen.core.BeanGeneratorBuilderTest.TestBean(id=" + this.getId() + ", name=" + this.getName() + ")";
        }
    }

    @Test
    public void withDefaultFieldPopulators() throws Exception {
        final Generator<TestBean> generator = BaseBuilders.aBean(TestBean.class).withDefaultFieldPopulators().build();

        final TestBean bean = generator.generate(0);
        Assert.assertEquals("bean 0 is not matching :" + bean, -2701295953664104481L, bean.getId());
        Assert.assertEquals("bean 0 is not matching :" + bean, "8OPSX85ojpvXYl", bean.getName());


        final Generator<TestBean> generator2 = BaseBuilders.aBean(TestBean.class).withDefaultFieldPopulators().build();
        Random rand = new Random();
        for (int i = 0; i < 100; i++) {
            long id = Math.abs(rand.nextLong());
            final TestBean bean1 = generator.generate(id);
            final TestBean bean2 = generator2.generate(id);
            Assert.assertTrue("Idempotent test failed : id=" + id, bean1.equals(bean2));
        }


    }

    @Test
    public void withFieldConfigured() throws Exception {
        final Generator<TestBean> generator = BaseBuilders.aBean(TestBean.class)
                .with("name", BaseBuilders.aString().matching("[ABC]{3}"))
                .withDefaultFieldPopulators().build();

        final TestBean bean = generator.generate(0);
        Assert.assertEquals("bean 0 is not matching :" + bean, -2701295953664104481L, bean.getId());
        Assert.assertEquals("bean 0 is not matching :" + bean, "BAB", bean.getName());


        final Generator<TestBean> generator2 = BaseBuilders.aBean(TestBean.class)
                .with("name", BaseBuilders.aString().matching("[ABC]{3}"))
                .withDefaultFieldPopulators().build();
        Random rand = new Random();
        for (int i = 0; i < 100; i++) {
            long id = Math.abs(rand.nextLong());
            final TestBean bean1 = generator.generate(id);
            final TestBean bean2 = generator2.generate(id);
            Assert.assertTrue("Idempotent test failed : id=" + id, bean1.equals(bean2));
        }


    }
}