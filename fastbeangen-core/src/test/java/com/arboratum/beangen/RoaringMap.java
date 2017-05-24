package com.arboratum.beangen;

/**
 * Created by gpicron on 16/12/2016.
 */
public class RoaringMap<VALUE> {

    private interface Container<VALUE> {

        VALUE get(int id);

        VALUE put(int id, VALUE value);
    }

    private class RoaringL1Container<VALUE> extends Short2ValueArrayMap<RoaringMap.Container<VALUE>> implements Container<VALUE> {

        @Override
        public VALUE get(int id) {
            final Container<VALUE> child = getValue(id);

            if (child == null) return null;
            return child.get(id);
        }

        @Override
        public VALUE put(int id, VALUE value) {
            final short l1 = (short) (id >> 16);
            final Container<VALUE> child;

            child = getOrInsert(l1, () -> new RoaringL2ArrayContainer<VALUE>());
            return child.put(id, value);
        }

    }
    private class RoaringL2ArrayContainer<VALUE> extends Short2ValueArrayMap<VALUE> implements Container<VALUE> {
        @Override
        public VALUE get(int id) {
            final short l2 = (short) (id & 0xFFFF);
            return get(l2);
        }

        @Override
        public VALUE put(int id, VALUE value) {
            final short l2 = (short) (id & 0xFFFF);
            return putValue(l2, value);
        }
    }

    private Container<VALUE> root = new RoaringL1Container<VALUE>();

    public VALUE put(int id, VALUE value) {
        return root.put(id, value);
    }

    public VALUE get(int id) {
        return root.get(id);
    }


}
