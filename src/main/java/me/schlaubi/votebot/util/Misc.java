package me.schlaubi.votebot.util;

import java.util.Map;
import java.util.Optional;

public class Misc {

    public static final String[] EMOTES = {"\uD83C\uDF4F", "\uD83C\uDF4E", "\uD83C\uDF50", "\uD83C\uDF4A", "\uD83C\uDF4B", "\uD83C\uDF4C", "\uD83C\uDF49", "\uD83C\uDF47", "\uD83C\uDF53", "\uD83C\uDF48", "\uD83C\uDF52", "\uD83C\uDF51", "\uD83C\uDF4D", "\uD83E\uDD5D",
            "\uD83E\uDD51", "\uD83C\uDF45", "\uD83C\uDF46", "\uD83E\uDD52", "\uD83E\uDD55", "\uD83C\uDF3D", "\uD83C\uDF36", "\uD83E\uDD54", "\uD83C\uDF60", "\uD83C\uDF30", "\uD83E\uDD5C \uD83C\uDF6F", "\uD83E\uDD50", "\uD83C\uDF5E",
            "\uD83E\uDD56", "\uD83E\uDDC0", "\uD83E\uDD5A", "\uD83C\uDF73", "\uD83E\uDD53", "\uD83E\uDD5E", "\uD83C\uDF64", "\uD83C\uDF57", "\uD83C\uDF56", "\uD83C\uDF55", "\uD83C\uDF2D", "\uD83C\uDF54", "\uD83C\uDF5F", "\uD83E\uDD59",
            "\uD83C\uDF2E", "\uD83C\uDF2F", "\uD83E\uDD57", "\uD83E\uDD58", "\uD83C\uDF5D", "\uD83C\uDF5C", "\uD83C\uDF72", "\uD83C\uDF65", "\uD83C\uDF63", "\uD83C\uDF71", "\uD83C\uDF5B", "\uD83C\uDF5A", "\uD83C\uDF59", "\uD83C\uDF58",
            "\uD83C\uDF62", "\uD83C\uDF61", "\uD83C\uDF67", "\uD83C\uDF68", "\uD83C\uDF66", "\uD83C\uDF70", "\uD83C\uDF82", "\uD83C\uDF6E", "\uD83C\uDF6D", "\uD83C\uDF6C", "\uD83C\uDF6B", "\uD83C\uDF7F", "\uD83C\uDF69", "\uD83C\uDF6A", "\uD83E\uDD5B",
            "\uD83C\uDF75", "\uD83C\uDF76", "\uD83C\uDF7A", "\uD83C\uDF7B", "\uD83E\uDD42", "\uD83C\uDF77", "\uD83E\uDD43", "\uD83C\uDF78", "\uD83C\uDF79", "\uD83C\uDF7E", "\uD83E\uDD44", "\uD83C\uDF74", "\uD83C\uDF7D"};

    public static <E, T> E getValueByKey(Map<E, T> map, T neededValue) {
        Optional<Map.Entry<E, T>> optional = map.entrySet().stream().filter(entry -> entry.getValue().equals(neededValue)).findFirst();
        return optional.map(Map.Entry::getKey).orElse(null);
    }
}
