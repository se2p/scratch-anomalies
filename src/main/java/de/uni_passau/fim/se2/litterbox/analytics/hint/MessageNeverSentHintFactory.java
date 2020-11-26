package de.uni_passau.fim.se2.litterbox.analytics.hint;

import de.uni_passau.fim.se2.litterbox.analytics.Hint;
import de.uni_passau.fim.se2.litterbox.analytics.bugpattern.MessageNeverSent;
import de.uni_passau.fim.se2.litterbox.utils.IssueTranslator;

import java.util.*;

public abstract class MessageNeverSentHintFactory {
    public static final String MESSAGE_IN_SAY_OR_THINK = "message_never_sent_say_think";

    public static Hint generateHint(String messageText, Map<String, Set<String>> sayText, Map<String, Set<String>> thinkText) {
        Hint hint;
        if (sayText.containsKey(messageText)) {
            hint = new Hint(MESSAGE_IN_SAY_OR_THINK);
            hint.setParameter(Hint.HINT_SAY_THINK, IssueTranslator.getInstance().getInfo("say"));
            hint.setParameter(Hint.HINT_SPRITE, generateSpritesText(sayText.get(messageText)));
        } else if (thinkText.containsKey(messageText)) {
            hint = new Hint(MESSAGE_IN_SAY_OR_THINK);
            hint.setParameter(Hint.HINT_SAY_THINK, IssueTranslator.getInstance().getInfo("think"));
            hint.setParameter(Hint.HINT_SPRITE, generateSpritesText(sayText.get(messageText)));
        } else {
            hint = new Hint(MessageNeverSent.NAME);
        }
        hint.setParameter(Hint.HINT_MESSAGE, messageText);

        return hint;
    }

    private static String generateSpritesText(Set<String> strings) {
        StringBuilder builder = new StringBuilder();
        int i = 1;
        for (String string : strings) {
            builder.append(string);
            if (i < strings.size()) {
                builder.append(", ");
            }
            i++;
        }
        return builder.toString();
    }

    public static Collection<String> getHintKeys() {
        List<String> keys = new ArrayList<>();
        keys.add(MessageNeverSent.NAME);
        keys.add(MESSAGE_IN_SAY_OR_THINK);
        return keys;
    }
}
