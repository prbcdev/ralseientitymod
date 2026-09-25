package dev.ralsei.entity.dialogue;

import java.util.List;
import java.util.Set;

public record ChatTrigger(String key, List<Set<String>> keywordGroups, List<DialogueMessage[]> responses) {
    // single-group
    public static ChatTrigger of(String key, List<DialogueMessage[]> responses, String... keywords) {
        return new ChatTrigger(key, List.<Set<String>>of(Set.of(keywords)), responses);
    }

    // multi-group (and/or)
    @SafeVarargs
    public static ChatTrigger ofGroups(String key, List<DialogueMessage[]> responses, Set<String>... groups) {
        return new ChatTrigger(key, List.<Set<String>>of(groups), responses);
    }

    int bestMatchSize(Set<String> words) {
        int best = 0;
        for (Set<String> group : keywordGroups) {
            if (group.size() > best && words.containsAll(group)) {
                best = group.size();
            }
        }
        return best;
    }
}