package com.arboratum.beangen.core;

import com.arboratum.beangen.BaseBuilders;
import com.arboratum.beangen.Generator;
import lombok.Data;
import org.junit.Assert;
import org.junit.Test;


import java.util.Random;

/**
 * Created by gpicron on 10/08/2016.
 */
public class BeanGeneratorBuilderTest {

    @Data
    public static class TestBean {
        private long id;
        private String name;
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