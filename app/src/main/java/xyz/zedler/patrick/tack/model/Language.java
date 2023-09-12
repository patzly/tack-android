package xyz.zedler.patrick.tack.model;

import java.util.Locale;
import xyz.zedler.patrick.tack.util.LocaleUtil;

public class Language implements Comparable<Language> {

  private final String code;
  private final String translators;
  private final String name;

  public Language(String codeTranslators) {
    String[] parts = codeTranslators.split("\n");
    code = parts[0];
    translators = parts[1];
    Locale locale = LocaleUtil.getLocaleFromCode(code);
    String displayName = locale.getDisplayName(locale);
    name = displayName.substring(0, 1).toUpperCase() + displayName.substring(1);
  }

  public String getCode() {
    return code;
  }

  public String getTranslators() {
    return translators;
  }

  public String getName() {
    return name;
  }

  @Override
  public int compareTo(Language other) {
    return name.toLowerCase().compareTo(other.getName().toLowerCase());
  }
}
