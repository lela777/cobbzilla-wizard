package org.cobbzilla.wizardtest;

import org.apache.commons.lang3.RandomStringUtils;

import java.util.Random;

import static org.cobbzilla.util.string.StringUtil.safeFunctionName;

public class TestNames {

    public static final String[] FRUITS = {
        "Apple", "Apricot", "Bilberry", "Blackberry", "Blueberry", "Boysenberry", "Cantaloupe", "Cherry",
        "Coconut", "Cranberry", "Date", "Dragonfruit", "Elderberry", "Fig", "Gooseberry", "Grape",
        "Grapefruit", "Guava", "Huckleberry", "Lemon", "Lime", "Lychee", "Mango", "Melon", "Cantaloupe",
        "Honeydew", "Watermelon", "Mulberry", "Nectarine", "Olive", "Orange", "Clementine", "Tangerine",
        "Papaya", "Passionfruit", "Peach", "Pear", "Persimmon", "Plum", "Pineapple", "Pomegranate",
        "Pomelo", "Raspberry", "Strawberry", "Banana", "Avocado"
    };

    public static final String[] NATIONALITIES = {
        "Afghan", "Albanian", "Algerian", "Andorran", "Angolan", "Argentinian", "Armenian", "Australian",
        "Austrian", "Azerbaijani", "Bahamian", "Bahraini", "Bangladeshi", "Barbadian", "Belarusian", "Belgian",
        "Belizean", "Beninese", "Bhutanese", "Bolivian", "Bosnian", "Botswanan", "Brazilian", "British", "Bruneian",
        "Bulgarian", "Burkinese", "Burmese", "Burundian", "Cambodian", "Cameroonian", "Canadian", "Cape", "Verdean",
        "Chadian", "Chilean", "Chinese", "Colombian", "Congolese", "Costa", "Rican", "Croatian", "Cuban", "Cypriot",
        "Czech", "Danish", "Djiboutian", "Dominican", "Dominican", "Ecuadorean", "Egyptian", "Salvadorean", "English",
        "Eritrean", "Estonian", "Ethiopian", "Fijian", "Finnish", "French", "Gabonese", "Gambian", "Georgian", "German",
        "Ghanaian", "Greek", "Grenadian", "Guatemalan", "Guinean", "Guyanese", "Haitian", "Dutch", "Honduran", "Hungarian",
        "Icelandic", "Indian", "Indonesian", "Iranian", "Iraqi", "Irish", "Italian", "Jamaican", "Japanese", "Jordanian",
        "Kazakh", "Kenyan", "Kuwaiti", "Laotian", "Latvian", "Lebanese", "Liberian", "Libyan", "Lithuanian", "Macedonian",
        "Madagascan", "Malawian", "Malaysian", "Maldivian", "Malian", "Maltese", "Mauritanian", "Mauritian", "Mexican",
        "Moldovan", "Monacan", "Mongolian", "Montenegrin", "Moroccan", "Mozambican", "Namibian", "Nepalese", "Dutch",
        "Nicaraguan", "Nigerien", "Nigerian", "North", "Korean", "Norwegian", "Omani", "Pakistani", "Panamanian", "Guinean",
        "Paraguayan", "Peruvian", "Philippine", "Polish", "Portuguese", "Qatari", "Romanian", "Russian", "Rwandan", "Saudi",
        "Scottish", "Senegalese", "Serbian", "Seychellois", "Sierra", "Leonian", "Singaporean", "Slovak", "Slovenian",
        "Somali", "South", "African", "South", "Korean", "Spanish", "Sri", "Lankan", "Sudanese", "Surinamese", "Swazi",
        "Swedish", "Swiss", "Syrian", "Taiwanese", "Tadjik", "Tanzanian", "Thai", "Togolese", "Trinidadian<br>", "Tunisian",
        "Turkish", "Turkmen", "Tuvaluan", "Ugandan", "Ukrainian", "British", "American", "Uruguayan", "Uzbek", "Vanuatuan",
        "Venezuelan", "Vietnamese", "Welsh", "Western", "Samoan", "Yemeni", "Yugoslav", "Zaïrean", "Zambian", "Zimbabwean"
    };

    public static final String[] ANIMALS = {
        "Elephant", "Alligator", "Kestrel", "Condor", "Arctic_Fox", "Bald_Eagle", "Black_Swan", "Duck", "Burrowing_Owl",
        "Sea_Lion", "Chinchilla", "Collared_Peccary", "Rabbit", "Snake", "Hedgehog", "Owl", "Bear", "Leopard", "Shark",
        "Panda", "Lynx", "Llama", "Marine_Toad", "Mouflon", "Musk_Ox", "Arrow_Frog", "Porcupine", "Tortoise", "Red_Panda",
        "Lemur", "Hawk", "Rhea", "Reindeer", "Siberian_Tiger", "Snow_Leopard", "Snowy_Owl", "Whites_Tree_Frog", "Wild_Boar",
        "Invertebrate", "Centipede", "Crustacean", "Hermit_Crab", "Wood_Louse", "Bullet_Ant", "Carpenter_Bee",
        "Honey_Pot_Ant", "Honeybee", "Snail", "Spider", "Scorpion", "Fish", "Zebra", "Tiger", "Crocodile", "Lizard",
        "Snake", "King_Cobra", "Turtle", "Bird", "Penguin", "Bat", "Cheetah", "Mongoose", "Jaguar", "Kinkajou",
        "Lion", "Otter", "Polar_Bear", "Puma", "Red_Panda", "Sand_Cat", "Slender", "Hyena", "Rhinoceros", "Gazelle",
        "Goat", "Hippopotamus", "Okapi", "Pig", "Warthog", "Hyrax", "Lemur", "Monkey", "Ape", "Chimpanzee", "Gorilla",
        "Kangaroo", "Opossum", "Giraffe", "Killer_Whale", "Horse", "Wolf", "Mole", "Dingo", "Deer", "Emu", "Crocodile",
        "Bobcat", "Lion", "Squirrel"
    };

    private static final Random rand = new Random();

    public static String safeName () {
        return safeFunctionName(nationality())+"-"+safeFunctionName(fruit())+"-"+RandomStringUtils.randomAlphanumeric(10);
    }

    public static String name() { return nationality() + " " + fruit(); }

    public static String fruit() { return FRUITS[rand.nextInt(FRUITS.length)]; }

    public static String nationality() { return NATIONALITIES[rand.nextInt(NATIONALITIES.length)]; }

    public static String animal() { return ANIMALS[rand.nextInt(ANIMALS.length)]; }

    public static String safeAnimal() { return animal()+"-"+RandomStringUtils.randomAlphanumeric(10); }
    public static String safeFruit() { return fruit()+"-"+RandomStringUtils.randomAlphanumeric(10); }
    public static String safeNationality() { return nationality()+"-"+RandomStringUtils.randomAlphanumeric(10); }

}
