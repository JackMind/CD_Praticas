import java.util.HashMap;

public class ObservableHashMap<K, V> extends HashMap<K, V>
{
    public ObservableHashMap()
    {
        super();
    }

    @Override
    public void clear()
    {
        super.clear();
    }

    @Override
    public V put(K key, V value)
    {
        //OnPut Logic
        //System.out.println("New Entry => " + key);
        return super.put(key, value);
    }

    @Override
    public V remove(Object key)
    {
        //OnRemove Logic
        //System.out.println("Removed Entry => " + key);
        V v = super.remove(key);
        return v;
    }
}
