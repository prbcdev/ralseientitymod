package dev.ralsei.entity.dialogue;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class DialoguePool {

    private static final List<DialogueMessage[]> GREETINGS = List.of(
            new DialogueMessage[] {
                    new DialogueMessage("Hello there!", TypingSpeed.NORMAL, "ralsei_mood_default_smile", TextAnimation.STILL,
                            TextSize.NORMAL, TextAlignment.LEFT, "wave"),
                    new DialogueMessage("I hope you've been doing great today!", TypingSpeed.NORMAL, "ralsei_mood_happy", TextAnimation.STILL)
            },
            new DialogueMessage[] {
                    new DialogueMessage("O..Oh! H-hi there...!", TypingSpeed.NORMAL, "ralsei_mood_happy", TextAnimation.STILL,
                            TextSize.NORMAL, TextAlignment.LEFT, "happy"),
                    new DialogueMessage("It's so nice to see you again~", TypingSpeed.NORMAL, "ralsei_mood_overjoyed", TextAnimation.STILL),
                    new DialogueMessage("How are you feeling today? Is everything fine?", TypingSpeed.NORMAL, "ralsei_mood_excited_smile", TextAnimation.STILL)
            },
            new DialogueMessage[] {
                    new DialogueMessage("W..W-Wait..!-", TypingSpeed.SLOW, "ralsei_mood_nervous", TextAnimation.SHAKE,
                            TextSize.NORMAL, TextAlignment.LEFT, "shocked"),
                    new DialogueMessage("WATCH OUT!!", TypingSpeed.FAST, "ralsei_mood_shocked", TextAnimation.WAVE,
                            TextSize.BIG, TextAlignment.CENTERED, "shocked"),
                    new DialogueMessage("...oh. ha, never mind, false alarm..-", TypingSpeed.NORMAL, "ralsei_mood_awkward_smile", TextAnimation.STILL),
                    new DialogueMessage("I thought there was a CREEPER behind you!", TypingSpeed.NORMAL, "ralsei_mood_slight_smile_unsure", TextAnimation.STILL, TextSize.NORMAL, TextAlignment.LEFT, "giggle"),
            },
            new DialogueMessage[] {
                    new DialogueMessage("Uhm, do you need help with anything..?", TypingSpeed.NORMAL, "ralsei_mood_reassured_smile", TextAnimation.STILL),
                    new DialogueMessage("I'm here for you if you need something!", TypingSpeed.NORMAL, "ralsei_mood_happy", TextAnimation.STILL),
            },
            new DialogueMessage[] {
                    new DialogueMessage("Sometimes.. when you are alone..", TypingSpeed.NORMAL, "ralsei_mood_eye_roll", TextAnimation.STILL),
                    new DialogueMessage("do you also wonder what your friends are doing?", TypingSpeed.NORMAL, "ralsei_mood_upset", TextAnimation.STILL, TextSize.NORMAL, TextAlignment.LEFT, "shy"),
            },
            new DialogueMessage[] {
                    new DialogueMessage("So.. you need two sticks and three cobblestone..", TypingSpeed.SLOW, "ralsei_mood_looking_sideways_smile", TextAnimation.STILL),
                    new DialogueMessage("to make a... cobblestone pickaxe?", TypingSpeed.NORMAL, "ralsei_mood_surprised", TextAnimation.STILL),
                    new DialogueMessage("(Ugh, In which order to they go into the CRAFTING TABLE again?", TypingSpeed.FAST, "ralsei_mood_uncertain_smirk", TextAnimation.WAVE, TextSize.SMALL, TextAlignment.CENTERED)
            },
            new DialogueMessage[] {
                    new DialogueMessage("Please, be careful. okay..?", TypingSpeed.NORMAL, "ralsei_mood_worried_smile", TextAnimation.STILL),
                    new DialogueMessage("This world is so massive and...", TypingSpeed.SLOW, "ralsei_mood_saddened", TextAnimation.STILL),
                    new DialogueMessage("all these MOBS make me worry for you when you're not here.", TypingSpeed.SLOW, "ralsei_mood_frown", TextAnimation.STILL),
                    new DialogueMessage("So please look after yourself out there, mhm?", TypingSpeed.NORMAL, "ralsei_mood_worried_smile", TextAnimation.STILL),

            },
            new DialogueMessage[] {
                    new DialogueMessage("Err, is everything um, alright with you..?", TypingSpeed.NORMAL, "ralsei_mood_worried_smile", TextAnimation.STILL),
                    new DialogueMessage("I..I just want to make sure you're doing OK.", TypingSpeed.NORMAL, "ralsei_mood_reassured_smile", TextAnimation.STILL)
            },
            new DialogueMessage[] {
                    new DialogueMessage("Hmm.. Susie would love all these exotic foods..", TypingSpeed.NORMAL, "ralsei_mood_awkward_smile", TextAnimation.STILL),
                    new DialogueMessage("I should note down all the ingredients to bake a CAKE...", TypingSpeed.NORMAL, "ralsei_mood_smart", TextAnimation.STILL),
                    new DialogueMessage("Wheat, sugar, eggs and...", TypingSpeed.SLOW, "ralsei_mood_upset", TextAnimation.STILL),
                    new DialogueMessage("@#%$!?", TypingSpeed.FAST, "ralsei_mood_confused", TextAnimation.SHAKE, TextSize.BIG, TextAlignment.CENTERED),
                    new DialogueMessage("THREE WHOLE BUCKETS OF MILK??!", TypingSpeed.FAST, "ralsei_mood_disgusted", TextAnimation.STILL, TextSize.NORMAL, TextAlignment.LEFT, "shocked"),
            },
            new DialogueMessage[] {
                    new DialogueMessage("Hmm, I wonder where my other friends are...", TypingSpeed.NORMAL, "ralsei_mood_uncertain_smirk", TextAnimation.STILL),
                    new DialogueMessage("(I sure hope they are safe.)", TypingSpeed.SLOW, "ralsei_mood_looking_away", TextAnimation.WAVE, TextSize.SMALL, TextAlignment.CENTERED),
            },
            new DialogueMessage[] {
                    new DialogueMessage("You know, that outfit could use a little embroidery.", TypingSpeed.NORMAL, "ralsei_mood_friendly_grin", TextAnimation.STILL),
                    new DialogueMessage("I could add a flower motive.. if you'd like!", TypingSpeed.NORMAL, "ralsei_mood_smart", TextAnimation.STILL),
                    new DialogueMessage("..wait a second...", TypingSpeed.SLOW, "ralsei_mood_upset", TextAnimation.STILL, TextSize.SMALL, TextAlignment.LEFT),
                    new DialogueMessage("I can make it look like the PEONY!", TypingSpeed.NORMAL, "ralsei_mood_overjoyed", TextAnimation.STILL)
            },
            new DialogueMessage[] {
                    new DialogueMessage("I was just thinking about er, you actually..", TypingSpeed.NORMAL, "ralsei_mood_blush", TextAnimation.STILL,
                            TextSize.NORMAL, TextAlignment.LEFT, "blush"),
                    new DialogueMessage("I hope you enjoy my company as much as I do yours.", TypingSpeed.NORMAL, "ralsei_mood_nervous_blush", TextAnimation.STILL,
                            TextSize.NORMAL, TextAlignment.LEFT, "blush"),
            },
            new DialogueMessage[] {
                    new DialogueMessage("Huh, this world sure is peaceful today..", TypingSpeed.SLOW, "ralsei_mood_pleasant_smile", TextAnimation.STILL),
                    new DialogueMessage("I like days like this..! Don't you too?", TypingSpeed.SLOW, "ralsei_mood_default_smile", TextAnimation.STILL)
            },
            new DialogueMessage[] {
                    new DialogueMessage("Um, do you think we're doing an OK job..?", TypingSpeed.NORMAL, "ralsei_mood_nervous", TextAnimation.STILL),
                    new DialogueMessage("In this MINECRAFT world, I mean.", TypingSpeed.NORMAL, "ralsei_mood_slight_smile_unsure", TextAnimation.STILL)
            },
            new DialogueMessage[] {
                    new DialogueMessage("O-Oh!.. you're back! finally~", TypingSpeed.FAST, "ralsei_mood_excited_smile", TextAnimation.STILL,
                            TextSize.NORMAL, TextAlignment.LEFT, "wave")
            },
            new DialogueMessage[] {
                    new DialogueMessage("You should show me how this world works sometime!", TypingSpeed.NORMAL, "ralsei_mood_smart", TextAnimation.STILL),
                    new DialogueMessage("I wanna try to a find a DIAMOND, too!", TypingSpeed.FAST, "ralsei_mood_playful", TextAnimation.STILL)
            },
            new DialogueMessage[] {
                    new DialogueMessage("I hope you're not uh, too tired from all that adventuring..", TypingSpeed.SLOW, "ralsei_mood_worried_smile", TextAnimation.STILL),
                    new DialogueMessage("There are so many MOBS in this world!", TypingSpeed.SLOW, "ralsei_mood_uncertain_smirk", TextAnimation.STILL)
            },
            new DialogueMessage[] {
                    new DialogueMessage("Ehehe, you caught me daydreaming again!", TypingSpeed.NORMAL, "ralsei_mood_disbelief", TextAnimation.STILL,
                            TextSize.NORMAL, TextAlignment.LEFT, "giggle")
            },
            new DialogueMessage[] {
                    new DialogueMessage("Once I get my hands on my very own...", TypingSpeed.NORMAL, "ralsei_mood_determined", TextAnimation.STILL),
                    new DialogueMessage("DIAMOND SWORD", TypingSpeed.SLOW, "ralsei_mood_yelling", TextAnimation.WAVE, TextSize.BIG, TextAlignment.CENTERED),
                    new DialogueMessage("there will be no stopping me!", TypingSpeed.NORMAL, "ralsei_mood_playful", TextAnimation.STILL),
                    new DialogueMessage("(I just need to remember the crafting recipe..)", TypingSpeed.FAST, "ralsei_mood_confused", TextAnimation.WAVE, TextSize.SMALL, TextAlignment.CENTERED)
            },
            new DialogueMessage[] {
                    new DialogueMessage("It's such a nice day to be a hero, don't you think?", TypingSpeed.NORMAL, "ralsei_mood_enthusiastic", TextAnimation.STILL),
                    new DialogueMessage("(Are there even heroes needed in this world?)", TypingSpeed.FAST, "ralsei_mood_eye_roll", TextAnimation.WAVE, TextSize.SMALL, TextAlignment.CENTERED)
            },
            new DialogueMessage[] {
                    new DialogueMessage("Um, do i still look presentable..?", TypingSpeed.NORMAL, "ralsei_mood_uncertain_smirk", TextAnimation.STILL),
                    new DialogueMessage("I was tidying up earlier and-", TypingSpeed.NORMAL, "ralsei_mood_reassured_smile", TextAnimation.STILL),
                    new DialogueMessage("... Argh, never mind. it's fine!", TypingSpeed.NORMAL, "ralsei_mood_pleasant_smile", TextAnimation.STILL),
            },
            new DialogueMessage[] {
                    new DialogueMessage("Thank you for being here with me..!", TypingSpeed.NORMAL, "ralsei_mood_reassured_smile", TextAnimation.STILL),
                    new DialogueMessage("How's your day been going so far?", TypingSpeed.NORMAL, "ralsei_mood_friendly_grin", TextAnimation.STILL)
            },
            new DialogueMessage[] {
                    new DialogueMessage("Ah! perfect timing, i was getting a little lonely..", TypingSpeed.NORMAL, "ralsei_mood_saddened", TextAnimation.STILL),
                    new DialogueMessage("..but i'm glad you're here now.", TypingSpeed.NORMAL, "ralsei_mood_pleasant_smile", TextAnimation.STILL)
            },
            new DialogueMessage[] {
                    new DialogueMessage("This world has so many pretty flowers...", TypingSpeed.NORMAL, "ralsei_mood_pleasant_smile", TextAnimation.STILL),
                    new DialogueMessage("I really like the...", TypingSpeed.NORMAL, "ralsei_mood_default_smile", TextAnimation.STILL),
                    new DialogueMessage("PEONY!", TypingSpeed.SLOW, "ralsei_mood_enthusiastic", TextAnimation.WAVE, TextSize.BIG, TextAlignment.CENTERED),
                    new DialogueMessage("They're so pretty, don't you think?!", TypingSpeed.SLOW, "ralsei_mood_slight_smile_unsure", TextAnimation.STILL)
            },
            new DialogueMessage[] {
                    new DialogueMessage("No matter what happens, I'm glad you're here with me!", TypingSpeed.SLOW, "ralsei_mood_reassured_smile", TextAnimation.STILL)
            },
            new DialogueMessage[] {
                    new DialogueMessage("So.. these, uh..", TypingSpeed.SLOW, "ralsei_mood_looking_sideways_smile", TextAnimation.STILL),
                    new DialogueMessage("VILLAGERS...", TypingSpeed.NORMAL, "ralsei_mood_nervous", TextAnimation.STILL),
                    new DialogueMessage("why are their noses so long..?", TypingSpeed.NORMAL, "ralsei_mood_shocked", TextAnimation.STILL),
                    new DialogueMessage("(Is that mean of me to ask?!)", TypingSpeed.FAST, "ralsei_mood_confused", TextAnimation.WAVE, TextSize.SMALL, TextAlignment.CENTERED)
            }
    );

    private static final List<DialogueMessage[]> WAKE_UP = List.of(
            new DialogueMessage[] {
                    new DialogueMessage("Wuh, wh..w-what?!", TypingSpeed.FAST, "ralsei_mood_shocked", TextAnimation.SHAKE,
                            TextSize.NORMAL, TextAlignment.LEFT, "shocked"),
                    new DialogueMessage("OH..!- It's just.. it's just you.", TypingSpeed.FAST, "ralsei_mood_worried_smile", TextAnimation.SHAKE),
                    new DialogueMessage("S-Sorry, ah. I must have dozed off...", TypingSpeed.NORMAL, "ralsei_mood_awkward_smile", TextAnimation.STILL,
                            TextSize.NORMAL, TextAlignment.LEFT, "shy")
            },
            new DialogueMessage[] {
                    new DialogueMessage("Huh, h-ha?! Erm, I'm awake, I'm awake!", TypingSpeed.FAST, "ralsei_mood_surprised", TextAnimation.SHAKE,
                            TextSize.NORMAL, TextAlignment.LEFT, "giggle"),
                    new DialogueMessage("I promise I was listening!", TypingSpeed.NORMAL, "ralsei_mood_awkward_smile", TextAnimation.STILL)
            },
            new DialogueMessage[] {
                    new DialogueMessage("Huh, Kris? Oh- no, it's you.. sorry!", TypingSpeed.FAST, "ralsei_mood_nervous_blush", TextAnimation.SHAKE),
                    new DialogueMessage("Wha- please tell me I didn't say anything embarrassing in my sleep!", TypingSpeed.NORMAL, "ralsei_mood_flustered", TextAnimation.STILL,
                            TextSize.NORMAL, TextAlignment.LEFT, "shy")
            }
    );

    private static final List<DialogueMessage[]> CAUGHT_SINGING = List.of(
            new DialogueMessage[] {
                    new DialogueMessage("O-Oh! umm... How much of that did you hear? Heh.", TypingSpeed.NORMAL, "ralsei_mood_blush", TextAnimation.STILL,
                            TextSize.NORMAL, TextAlignment.LEFT, "blush")
            },
            new DialogueMessage[] {
                    new DialogueMessage("Ehehe... just trying to pass time, you know?", TypingSpeed.NORMAL, "ralsei_mood_awkward_smile", TextAnimation.STILL,
                            TextSize.NORMAL, TextAlignment.LEFT, "giggle")
            },
            new DialogueMessage[] {
                    new DialogueMessage("Ahh! I, um, didn't realize anyone was listening..", TypingSpeed.NORMAL, "ralsei_mood_blush", TextAnimation.STILL),
                    new DialogueMessage("It's just a little tune I came up with, pfft.", TypingSpeed.SLOW, "ralsei_mood_pleasant_smile", TextAnimation.STILL,
                            TextSize.NORMAL, TextAlignment.LEFT, "blush")
            }
    );

    public static DialogueMessage[] randomCaughtSinging() {
        return CAUGHT_SINGING.get(ThreadLocalRandom.current().nextInt(CAUGHT_SINGING.size()));
    }

    public static DialogueMessage[] randomWakeUp() {
        return WAKE_UP.get(ThreadLocalRandom.current().nextInt(WAKE_UP.size()));
    }

    private DialoguePool() {}

    public static DialogueMessage[] randomGreeting() {
        return GREETINGS.get(ThreadLocalRandom.current().nextInt(GREETINGS.size()));
    }

    public static final DialogueMessage[] FOLLOW_START_MESSAGES = {
            new DialogueMessage("I will follow you from now on!", TypingSpeed.NORMAL, "ralsei_mood_pleasant_smile", TextAnimation.STILL),
            new DialogueMessage("Just take the lead and show me where we'll be going.", TypingSpeed.NORMAL, "ralsei_mood_excited_smile", TextAnimation.STILL)
    };
    public static final DialogueMessage[] FOLLOW_STOP_MESSAGES = {
            new DialogueMessage("Let me stay here, I will wait for you until you're back.", TypingSpeed.NORMAL, "ralsei_mood_pleasant_smile", TextAnimation.STILL)
    };

    private static final String NAME_TRIGGER = "ralsei";

    private static final List<ChatTrigger> CHAT_TRIGGERS = List.of(
            ChatTrigger.of("how_are_you", List.<DialogueMessage[]>of(
                    new DialogueMessage[] {
                            new DialogueMessage("I'm doing well, thank you for asking!", TypingSpeed.NORMAL, "ralsei_mood_happy", TextAnimation.STILL)
                    },
                    new DialogueMessage[] {
                            new DialogueMessage("Oh, um, I'm fine! Just enjoying the scenery.", TypingSpeed.NORMAL, "ralsei_mood_pleasant_smile", TextAnimation.STILL)
                    }
            ), "how", "you"),
            ChatTrigger.of("greeting", List.<DialogueMessage[]>of(
                    new DialogueMessage[] {
                            new DialogueMessage("Oh! Hello there!", TypingSpeed.NORMAL, "ralsei_mood_happy", TextAnimation.STILL)
                    }
            ), "hello"),
            ChatTrigger.ofGroups("stinks", List.<DialogueMessage[]>of(
                    new DialogueMessage[] {
                            new DialogueMessage("H-Hey! That's not very nice...", TypingSpeed.NORMAL, "ralsei_mood_upset", TextAnimation.STILL)
                    },
                    new DialogueMessage[] {
                            new DialogueMessage("I took a bath this morning, I promise!", TypingSpeed.NORMAL, "ralsei_mood_pout", TextAnimation.STILL)
                    }
            ), Set.of("stinks"), Set.of("smells"))
            // future entries: same pattern, List.<DialogueMessage[]>of(...) instead of List.of(...)
    );

    private static final List<DialogueMessage[]> CHAT_FALLBACK = List.of(
            new DialogueMessage[] {
                    new DialogueMessage("Hm? Did you need something?", TypingSpeed.NORMAL, "ralsei_mood_looking_sideways_smile", TextAnimation.STILL)
            },
            new DialogueMessage[] {
                    new DialogueMessage("Yes? I'm listening!", TypingSpeed.NORMAL, "ralsei_mood_default_smile", TextAnimation.STILL)
            }
    );

    public static boolean mentionsRalsei(Set<String> words) {
        return words.contains(NAME_TRIGGER);
    }

    public static DialogueMessage[] concat(DialogueMessage[] first, DialogueMessage[] second) {
        DialogueMessage[] combined = new DialogueMessage[first.length + second.length];
        System.arraycopy(first, 0, combined, 0, first.length);
        System.arraycopy(second, 0, combined, first.length, second.length);
        return combined;
    }

    public static String matchChatKey(Set<String> words) {
        String bestKey = "";
        int bestScore = 0;
        for (ChatTrigger trigger : CHAT_TRIGGERS) {
            int score = trigger.bestMatchSize(words);
            if (score > bestScore) {
                bestKey = trigger.key();
                bestScore = score;
            }
        }
        return bestKey;
    }

    public static DialogueMessage[] randomForChatKey(String key) {
        for (ChatTrigger trigger : CHAT_TRIGGERS) {
            if (trigger.key().equals(key)) {
                List<DialogueMessage[]> pool = trigger.responses();
                return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
            }
        }
        return CHAT_FALLBACK.get(ThreadLocalRandom.current().nextInt(CHAT_FALLBACK.size()));
    }
    private static final List<DialogueMessage[]> DYING_MESSAGES = List.<DialogueMessage[]>of(
            new DialogueMessage[] {
                    new DialogueMessage("...I think... that's enough for today.", TypingSpeed.SLOW, "ralsei_mood_saddened", TextAnimation.SCARED,
                            TextSize.NORMAL, TextAlignment.LEFT, "dying")
            }
            // placeholder
    );

    public static DialogueMessage[] randomDying() {
        return DYING_MESSAGES.get(ThreadLocalRandom.current().nextInt(DYING_MESSAGES.size()));
    }
}


