/*
Copyright (c) 2014-2015 F-Secure
See LICENSE for details
*/
package cc.softwarefactory.lokki.android.apprater;

import java.util.HashMap;
import java.util.Map;


public class AppRaterStrings {

    //public static final ArrayList<String,  languages = {{"en"}};
    public static final Map<String, HashMap<String, String>> translations = initialize();

    private static Map<String, HashMap<String, String>> initialize() {

        HashMap<String, HashMap<String, String>> result = new HashMap<String, HashMap<String, String>>();

        // ENGLISH
        HashMap<String, String> en = new HashMap<String, String>();
        en.put("give_us_feedback", "Give us feedback");
        en.put("do_you_like_this_app", "Do you like this app?");
        en.put("yes", "Yes");
        en.put("no", "No");
        en.put("feedback_title", "Send feedback");
        en.put("error_send_email_title", "Cannot send email");
        en.put("error_send_email_message", "We could not find your email app.\nContact us at: lokki-feedback@f-secure.com");
        en.put("ok", "Ok");
        en.put("rate_us", "Rate us");
        en.put("if_you_enjoy", "If you enjoy using this app, please take a moment to rate it. Thanks for your support!");
        en.put("remind_me_later", "Remind me later");
        en.put("no_thanks", "No, thanks");
        en.put("rate", "Rate");
        result.put("en", en);
        // -------------------------------------------------------------------
        // FINNISH
        HashMap<String, String> fi = new HashMap<String, String>();
        fi.put("give_us_feedback", "Lähetä palautetta");
        fi.put("do_you_like_this_app", "Pidätkö Lokki-sovelluksesta?");
        fi.put("yes", "Kyllä");
        fi.put("no", "Ei");
        fi.put("feedback_title", "Arvostele Lokki");
        fi.put("error_send_email_title", "Sähköpostiongelmia");
        fi.put("error_send_email_message", "Sähköpostia ei voi lähettää, koska sähköpostisovelluksen asetukset eivät ole kunnossa!");
        fi.put("ok", "Ok");
        fi.put("rate_us", "Arvostele Lokki");
        fi.put("if_you_enjoy", "Jos pidät Lokista, voisitko käydä sovelluskaupassa antamassa sille tähtiä? Kiitos tuestasi!");
        fi.put("remind_me_later", "Muistuta minua myöhemmin");
        fi.put("no_thanks", "Ei kiinnosta");
        fi.put("rate", "Anna tähtiä");
        result.put("fi", fi);
        // -------------------------------------------------------------------
        // RUSSIAN
        HashMap<String, String> ru = new HashMap<String, String>();
        ru.put("give_us_feedback", "Оцените Lokki");
        ru.put("do_you_like_this_app", "Вам нравится Lokki?");
        ru.put("yes", "Да");
        ru.put("no", "Нет");
        ru.put("feedback_title", "Послать оценку");
        ru.put("error_send_email_title", "Не могу послать email");
        ru.put("error_send_email_message", "Мы не смогли найти ваше email приложение.\nПошлите нам письмо на: https://github.com/TheSoftwareFactory/lokki-android/");
        ru.put("ok", "Ok");
        ru.put("rate_us", "Оценить");
        ru.put("if_you_enjoy", "Если Вам нравится Lokki, оцените нас, пожалуйста. Это займет всего минуту. Спасибо за поддержку!");
        ru.put("remind_me_later", "Напомнить позже");
        ru.put("no_thanks", "Нет, спасибо");
        ru.put("rate", "Оценить");
        result.put("ru", ru);
        // -------------------------------------------------------------------
        // SWEDISH
        HashMap<String, String> sv = new HashMap<String, String>();
        sv.put("give_us_feedback", "Ge oss feedback");
        sv.put("do_you_like_this_app", "Gillar du Lokki-appen?");
        sv.put("yes", "Ja");
        sv.put("no", "Nej");
        sv.put("feedback_title", "Värdera Lokki");
        sv.put("error_send_email_title", "Mailproblem");
        sv.put("error_send_email_message", "Kan inte skicka mail på grund av felaktiga inställningar!");
        sv.put("ok", "Ok");
        sv.put("rate_us", "Värdera Lokki");
        sv.put("if_you_enjoy", "Om du gillar Lokki, vänligen ta en stund att värdera den. Tack för ditt stöd!");
        sv.put("remind_me_later", "Påminn mig senare");
        sv.put("no_thanks", "Nej tack");
        sv.put("rate", "Värdera Lokki");
        result.put("sv", sv);
        // -------------------------------------------------------------------

        return result;
    }
}
