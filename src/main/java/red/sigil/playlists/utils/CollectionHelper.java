package red.sigil.playlists.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public class CollectionHelper {

  public static <K, V> Map<K, V> toMap(List<V> items, Function<V, K> selector) {
    Map<K, V> result = new HashMap<>();
    for (V item : items) {
      result.put(selector.apply(item), item);
    }
    return result;
  }

  public static <T> T findFirst(Collection<T> items, Predicate<T> selector) {
    for (T item : items) {
      if (selector.test(item))
        return item;
    }
    return null;
  }
}
