package com.example.scrabble;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import java.io.*;
import java.util.*;

interface Scorable    { int     getScoreValue(); }
interface Validatable { boolean isValid(); }

class InvalidWordException      extends Exception { InvalidWordException(String m)      { super(m); } }
class InvalidPlacementException extends Exception { InvalidPlacementException(String m) { super(m); } }

class Tile implements Serializable, Validatable {
    private char letter; private int value; private boolean blank;
    Tile()                             { this(' ', 0, true); }
    Tile(char l, int v)                { this(l, v, false); }
    Tile(char l, int v, boolean blank) { letter=l; value=v; this.blank=blank; }
    Tile(Tile o)                       { this(o.letter, o.value, o.blank); }

    char    getLetter()          { return letter; }
    void    setLetter(char l)    { letter = l; }
    int     getValue()           { return value; }
    boolean isBlank()            { return blank; }
    void    assignLetter(char c) { if (blank) letter = Character.toUpperCase(c); }
    @Override public boolean isValid()  { return letter != ' '; }
    @Override public String  toString() { return blank?"["+letter+"*]":"["+letter+":"+value+"]"; }
}

abstract class Square implements Serializable, Scorable {
    protected Tile tile; protected final int row, col;
    Square(int r, int c) { row=r; col=c; }
    boolean isEmpty()         { return tile==null; }
    Tile    getTile()         { return tile; }
    void    placeTile(Tile t) { tile=t; }
    int     getRow()          { return row; }
    int     getCol()          { return col; }
    abstract int    getLetterMultiplier();
    abstract int    getWordMultiplier();
    abstract String getLabel();
    @Override public int getScoreValue() { return tile==null?0:tile.getValue()*getLetterMultiplier(); }
}

class NormalSquare extends Square {
    NormalSquare(int r, int c) { super(r,c); }
    @Override public int    getLetterMultiplier() { return 1; }
    @Override public int    getWordMultiplier()   { return 1; }
    @Override public String getLabel()            { return ""; }
}

class PremiumSquare extends Square {
    enum BonusType { DOUBLE_LETTER, TRIPLE_LETTER, DOUBLE_WORD, TRIPLE_WORD, CENTER_STAR }
    protected final BonusType bonus; private boolean used;
    PremiumSquare(int r, int c, BonusType b) { super(r,c); bonus=b; }
    BonusType getBonusType() { return bonus; }
    @Override public void placeTile(Tile t) { super.placeTile(t); used=true; }
    @Override public int getLetterMultiplier() {
        if (used) return 1;
        return switch(bonus){ case DOUBLE_LETTER->2; case TRIPLE_LETTER->3; default->1; };
    }
    @Override public int getWordMultiplier() {
        return switch(bonus){ case DOUBLE_WORD,CENTER_STAR->2; case TRIPLE_WORD->3; default->1; };
    }
    @Override public String getLabel() {
        return switch(bonus){ case DOUBLE_LETTER->"DL"; case TRIPLE_LETTER->"TL";
                              case DOUBLE_WORD->"DW";   case TRIPLE_WORD->"TW"; default->"★"; };
    }
}

class CenterSquare extends PremiumSquare {
    CenterSquare(int r, int c) { super(r, c, BonusType.CENTER_STAR); }
    @Override public String getLabel() { return "★"; }
}

class Board implements Serializable {
    static final int SIZE = 15;
    private final Square[][] grid = new Square[SIZE][SIZE];
    Board() { buildLayout(); }
    Square  getSquare(int r, int c)  { return grid[r][c]; }
    boolean isInBounds(int r, int c) { return r>=0&&r<SIZE&&c>=0&&c<SIZE; }
    private void buildLayout() {
        for (int r=0;r<SIZE;r++) for (int c=0;c<SIZE;c++) grid[r][c]=new NormalSquare(r,c);
        bonus(PremiumSquare.BonusType.TRIPLE_WORD,
            new int[][]{{0,0},{0,7},{0,14},{7,0},{7,14},{14,0},{14,7},{14,14}});
        bonus(PremiumSquare.BonusType.DOUBLE_WORD,
            new int[][]{{1,1},{2,2},{3,3},{4,4},{10,4},{11,3},{12,2},{13,1},{1,13},{2,12},{3,11},{4,10},{10,10},{11,11},{12,12},{13,13}});
        bonus(PremiumSquare.BonusType.TRIPLE_LETTER,
            new int[][]{{1,5},{1,9},{5,1},{5,5},{5,9},{5,13},{9,1},{9,5},{9,9},{9,13},{13,5},{13,9}});
        bonus(PremiumSquare.BonusType.DOUBLE_LETTER,
            new int[][]{{0,3},{0,11},{2,6},{2,8},{3,0},{3,7},{3,14},{6,2},{6,6},{6,8},{6,12},{7,3},{7,11},{8,2},{8,6},{8,8},{8,12},{11,0},{11,7},{11,14},{12,6},{12,8},{14,3},{14,11}});
        int m=SIZE/2; grid[m][m]=new CenterSquare(m,m);
    }
    private void bonus(PremiumSquare.BonusType t, int[][] pos) {
        for (int[] p:pos) grid[p[0]][p[1]]=new PremiumSquare(p[0],p[1],t);
    }
}

class TileBag implements Serializable {
    private static int totalEverCreated = 0;
    private final List<Tile> tiles = new ArrayList<>();
    TileBag() {
        int[][] dist = {{9,1},{2,3},{2,3},{4,2},{12,1},{2,4},{3,2},{2,4},{9,1},{1,8},
                        {1,5},{4,1},{2,3},{6,1},{8,1},{2,3},{1,10},{6,1},{4,1},{6,1},
                        {4,1},{2,4},{2,4},{1,8},{2,4},{1,10}};
        for (int i=0;i<26;i++)
            for (int j=0;j<dist[i][0];j++) { tiles.add(new Tile((char)('A'+i),dist[i][1])); totalEverCreated++; }
        for (int i=0;i<2;i++) { tiles.add(new Tile()); totalEverCreated++; }
        Collections.shuffle(tiles);
    }
    static int     getTotalEverCreated() { return totalEverCreated; }
    boolean        isEmpty()             { return tiles.isEmpty(); }
    int            remaining()           { return tiles.size(); }
    List<Tile>     draw(int n)           {
        List<Tile> d=new ArrayList<>();
        for (int i=0;i<n&&!tiles.isEmpty();i++) d.add(tiles.remove(tiles.size()-1));
        return d;
    }
    void returnTiles(List<Tile> t) { tiles.addAll(t); Collections.shuffle(tiles); }
}

class Player implements Serializable, Scorable {
    private static int count = 0;
    private final int id; private final String name;
    private int score; private final List<Tile> rack = new ArrayList<>();
    Player(String n) { name=n; score=0; count++; id=count; }
    static int    getCount()             { return count; }
    int           getId()                { return id; }
    String        getName()              { return name; }
    int           getScore()             { return score; }
    List<Tile>    getRack()              { return rack; }
    void addScore(int pts)               { score+=pts; }
    void addScore(Scorable s)            { score+=s.getScoreValue(); }
    @Override public int getScoreValue() { return score; }
    void    addTilesToRack(List<Tile> t) { rack.addAll(t); }
    boolean removeFromRack(Tile t)       { return rack.remove(t); }
    void    removeFromRack(List<Tile> t) { rack.removeAll(t); }
    @Override public String toString()   { return name+"("+score+")"; }
}

class GameRecord<T extends Scorable> {
    private final String label; private final T item;
    GameRecord(String l, T i) { label=l; item=i; }
    String getLabel() { return label; }
    T      getItem()  { return item; }
    int    getScore() { return item.getScoreValue(); }
    @Override public String toString() { return label+"["+getScore()+"]"; }
}

class WordHistory implements Iterable<String> {
    private final LinkedList<String> list = new LinkedList<>();
    void   add(String w) { list.addFirst(w); }
    int    size()        { return list.size(); }
    String latest()      { return list.isEmpty()?"none":list.getFirst(); }
    @Override public Iterator<String> iterator() { return list.iterator(); }
}

class Dictionary {
    private final Set<String>         words  = new HashSet<>();
    private final Map<String,Integer> looked = new HashMap<>();
    Dictionary() { loadWords(); }
    boolean isValidWord(String w) {
        w = w.toUpperCase();
        looked.merge(w, 1, Integer::sum);
        return words.contains(w);
    }
    int size()                           { return words.size(); }
    Map<String,Integer> getLookupStats() { return Collections.unmodifiableMap(looked); }
    private void loadWords() {
        String raw =
            "AA AB AD AE AG AH AI AM AN AR AS AT AW AX AY BA BE BI BO BY DA DE DO ED EF EH EL " +
            "EM EN ER ES ET EX FA GO HA HE HI HM HO ID IF IN IS IT JO KA KI LA LI LO MA ME MI " +
            "MO MU MY NA NE NO NU OD OE OF OH OK OM ON OP OR OS OW OX OY PA PE PI QI RE SH SI " +
            "SO TA TI TO UH UM UN UP US UT WE WO XI XU YA YE YO ZA " +
            "ACE ACT ADD ADO AGE AGO AID AIM AIR ALE ALL AMP AND ANT ANY APE ARC ARE ARK ARM ART " +
            "ASH ASK ATE AWE AWL AWN AXE AYE BAD BAG BAN BAR BAT BAY BED BEG BET BID BIG BIN BIT " +
            "BOA BOG BOW BOX BOY BUD BUG BUM BUN BUS BUT BUY BYE CAB CAN CAP CAR CAT CEE COB COD " +
            "COG CON COP COT COW COY CRY CUB CUE CUP CUR CUT DAD DAM DAY DEN DEW DIG DIM DIN DIP " +
            "DOC DOE DOG DOT DRY DUB DUE DUG DUN DUO EAR EAT EEL EGG EGO ELK ELL ELM EMU END ERA " +
            "ERG ERR ETA EWE EYE FAD FAN FAR FAT FAX FAY FED FEN FEW FIG FIN FIR FIT FIX FLU FLY " +
            "FOB FOE FOG FOX FRO FRY FUN FUR GAB GAG GAP GAS GAY GEL GEM GET GIG GIN GNU GOB GOD " +
            "GUM GUN GUT GUY GYM HAD HAM HAS HAT HAW HAY HEM HEN HER HET HEW HEX HID HIM HIP HIT " +
            "HOE HOG HOP HOT HOW HOY HUB HUE HUG HUM HUT ICE ICY ILL IMP INK INN ION IRE IRK JAB " +
            "JAG JAM JAR JAW JAY JET JIG JOB JOE JOG JOT JOY JUG JUT KEG KEY KID KIT LAB LAD LAP " +
            "LAW LAX LAY LEA LED LEE LEG LET LID LIE LIP LIT LOB LOG LOO LOP LOT LOW LUG MAD MAN " +
            "MAP MAR MAT MAY MED MEN MET MEW MID MIX MOB MOD MOM MOP MOW MUD MUG NAB NAG NAP NAY " +
            "NET NEW NIB NIP NIT NOB NOD NOR NOT NOW NUB NUN NUT OAF OAK OAR OAT ODD ODE OHM OLD " +
            "OLE ONE OPT ORB ORC ORE OWE OWL OWN PAD PAL PAN PAR PAT PAW PAY PEA PEG PEN PET PEW " +
            "PHI PIE PIG PIN PIT PLY POD POP POT POW PRO PRY PUB PUN PUP PUT RAG RAN RAP RAT RAW " +
            "RAY RED RIB RID RIG RIM RIP ROB ROD ROE ROT ROW RUB RUG RUM RUN RUT SAD SAG SAP SAT " +
            "SAW SAY SEA SEE SET SEW SHE SHY SIN SIP SIR SIT SKI SKY SOB SOD SON SOP SOW SOY SPA " +
            "SPY SUB SUM SUN SUP TAB TAD TAG TAP TAR TEA TED TEE TEN TIN TIP TOE TON TOO TOP TOT " +
            "TOW TOY TUB TUG TUM TUN URN USE VAN VAT VEE VET VIA VIE VIM VOW WAD WAG WAN WAR WAX " +
            "WAY WEB WED WET WHO WHY WIG WIN WIT WIZ WOE WOK YAK YAM YAP YAW YEA YES YET YEW YIN " +
            "YOU ZAP ZED ZEE ZOO " +
            "ABLE ACHE ACID ACRE AGED AIDE AKIN ALOE ALTO AMID ANTE ANTI APEX ARCH AREA ARID ARMY " +
            "ARTS ATOM AUNT AUTO AVID AWAY AWRY BABE BABY BACK BAKE BALD BALE BALL BAND BANE BANG " +
            "BANK BARE BARN BASE BASH BASS BATH BEAD BEAK BEAM BEAN BEAR BEAT BEEF BEER BELL BELT " +
            "BEND BEST BILE BILL BIND BIRD BITE BLOW BLUE BLUR BOLD BOLT BOND BONE BOOK BOOM BORE " +
            "BORN BOSS BOUT BRAG BRAN BREW BUCK BUFF BULB BULK BULL BUMP BUNK BURN BUZZ CAGE CAKE " +
            "CALF CALL CALM CAME CAMP CANE CAPE CARD CARE CART CASH CAST CAVE CELL CHEW CHIP CHOP " +
            "CITE CITY CLAM CLAP CLAY CLIP CLOT CLUB CLUE COAL COAT COIL COIN COLD COLT COMB COME " +
            "CONE COOK COOL COPE COPY CORD CORE CORK CORN COST CRAB CRAM CREW CROP CROW CUBE CULT " +
            "CURE CURL CUTE DAMP DARE DARK DART DASH DAZE DEAD DEAF DEAL DEAN DEBT DECK DEED DEEP " +
            "DEER DENT DENY DINE DIRE DIRT DISH DISK DIVE DOCK DOME DONE DOVE DOWN DRAG DRAW DRIP " +
            "DROP DRUG DRUM DUAL DULL DUMB DUNE DUNK DUSK DUST DUTY EARL EARN EAST EASY EDGE EMIT " +
            "EPIC EVEN EVER EVIL EXAM FACE FACT FADE FAIL FAIR FAKE FAME FANG FARM FAST FATE FAWN " +
            "FEAR FEAT FEEL FEET FELL FELT FERN FILL FILM FIND FINE FIRE FIRM FIST FLAG FLAP FLAT " +
            "FLAW FLEA FLEW FLIP FLOG FLOP FLOW FOAM FOLD FOLK FOND FONT FOOD FOOL FOOT FORK FORM " +
            "FORT FOUL FOUR FOWL FRAY FREE FROG FUEL FULL FUSE FUSS GAIT GALE GALL GAZE GEAR GILL " +
            "GIRL GIVE GLEE GLOB GLUE GLUM GOAL GOAT GOLD GOLF GONE GOOD GRAB GRIM GRIN GRIP GRIT " +
            "GULL GULP GUNS GURU GUSH GUST GUTS GUYS HAIL HAIR HALE HALF HALL HALO HALT HAND HANG " +
            "HARD HARE HARM HARP HASH HATE HAUL HAWK HAZE HEAL HEAP HEAT HEEL HELD HELM HELP HERD " +
            "HERE HIKE HILL HINT HIRE HIVE HOAX HOLD HOLE HOLY HONE HOOD HOOK HOPE HORN HOSE HOST " +
            "HUGE HULL HUMP HUNT HURL HURT HUSK IDLE INCH IRON ISLE JACK JADE JAIL JEST JOIN JOKE " +
            "JOLT JUNK JURY JUST KEEN KEEP KILL KILN KIND KING KINK KISS KNIT KNOB KNOT KNOW LACK " +
            "LAME LAMP LAND LANE LASH LAST LATE LAVA LAWN LEAD LEAF LEAK LEAN LEAP LEFT LEND LESS " +
            "LICK LIFE LIFT LIKE LIMB LIME LIMP LINE LINK LION LIST LIVE LOAD LOAN LOCK LOFT LONE " +
            "LONG LOOK LOOM LOON LOOP LORE LOSS LOST LOUD LOVE LUCK LULL LUMP LUNG LURE LURK LUST " +
            "MADE MAKE MALL MALT MANY MARK MASH MASK MAST MATE MAZE MEAL MEAN MELT MERE MESH MILD " +
            "MILE MILL MIND MINE MINT MISS MIST MOAN MOCK MODE MOLE MONK MOOD MOPE MORN MOST MUCH " +
            "MULE MUST MUTE NAIL NAME NAVY NEAR NEAT NECK NEED NEST NEXT NICE NICK NINE NODE NONE " +
            "NOOK NOON NORM NOSE NOTE NOUN NUDE NULL NUMB NUTS OATH OBEY ODDS OMEN ONCE ONLY OPEN " +
            "OVEN OXEN PACE PACK PAGE PAID PAIL PAIN PAIR PALE PALM PANT PARK PART PAST PATH PAVE " +
            "PAWN PEAK PEEL PEER PELT PERK PEST PICK PIER PILE PILL PINE PINK PINT PIPE PLAN PLOD " +
            "PLOP PLOT PLOW PLUM PLUS POEM POET POKE POLE POLL POND POOL POOR PORE PORK PORT POSE " +
            "POST POUR PREY PRIM PROD PROP PULL PUMP PUNT PURE PUSH PUTT RACE RACK RAFT RAGE RAID " +
            "RAIL RAIN RAKE RAMP RANK RANT RASH RATE RAVE REAL REAP REEL REIN REND RENT REST RICE " +
            "RICH RIDE RIFE RIFT RING RIOT RIPE RISK ROAM ROAR ROBE ROCK RODE ROLE ROLL ROOK ROPE " +
            "ROSE ROUT RUIN RULE RUNE RUNG RUGS RUNS RUSH RUST RUTS SAFE SAGE SAIL SAKE SALE SALT " +
            "SAME SAND SANE SANG SANK SASH SATE SEAM SEAT SEED SEEK SEEM SEEN SELF SELL SHIN SHIP " +
            "SHOE SHOT SHOW SHUT SICK SIDE SIGH SILK SILL SING SINK SITE SIZE SKID SKIM SKIN SKIP " +
            "SLAM SLAP SLAT SLEW SLIM SLIP SLIT SLOB SLOG SLOP SLOT SLOW SLUM SOAK SOAR SOCK SOFA " +
            "SOIL SOLD SOLE SOME SONG SORE SORT SOUL SOUR SPAN SPAR SPED SPIN SPIT SPOT SPUN SPUR " +
            "STAB STAG STAR STAY STEM STEP STEW STIR STUB STUD STUN SUCK SUIT SULK SUNG SUNK SURE " +
            "SURF SWAP SWAT SWAY SWIM TAIL TAKE TALE TALK TALL TAME TANK TART TASK TAUT TEAM TEAR " +
            "TEEN TELL TEND TENT TERM TEST TICK TIDE TIER TILL TILT TINE TINY TIRE TOAD TOIL TOLL " +
            "TOMB TOME TONE TOOL TORE TORN TOSS TOUR TOWN TRAP TRAY TRIM TRIO TRIP TRUE TUBE TUCK " +
            "TUFT TUGS TUNE TURF TUSK TWIG TWIN UGLY UNDO UNIT UPON URGE USED USER VAIN VALE VARY " +
            "VASE VAST VEIL VEIN VERY VEST VETO VIAL VIEW VILE VINE VOID VOTE WADE WAGE WAIL WAIT " +
            "WAKE WANE WARD WARN WART WARY WATT WAVE WAXY WEAK WEAN WEED WELD WELL WELT WICK WIFE " +
            "WILD WILE WINE WING WINK WIRE WISE WISH WISP WOKE WOMB WOOL WORD WORE WORM WORN WRAP " +
            "WREN YAWN YEAR YELL YOGA YOKE YOUR ZEAL ZERO ZEST ZONE ZOOM " +
            "ABBEY ABIDE ABORT ABUSE ACUTE ADMIT ADOPT ADULT AFTER AGILE AISLE ALARM ALBUM ALERT " +
            "ALIEN ALIKE ALIVE ALLOW ALONE ALONG ALTER AMAZE AMEND AMPLE ANGEL ANGER ANGLE ANGRY " +
            "ANVIL APART APRON ARENA ARGUE ARISE ARMOR AROMA AROSE ARRAY ARSON ASIDE ATLAS ATONE " +
            "ATTIC AUDIT AVOID AWAIT AWAKE AWFUL BADGE BEAST BELOW BENCH BLACK BLADE BLAME BLAND " +
            "BLANK BLAST BLAZE BLEAK BLEND BLESS BLIND BLINK BLISS BLOCK BLOOD BLOOM BLOWN BLUFF " +
            "BLUNT BOARD BOAST BOOST BOUND BRACE BRAND BRAVE BREAD BREAK BRIDE BRISK BROIL BROOD " +
            "BRUSH BUNCH BURST BUYER CANDY CARGO CARRY CATCH CAUSE CEASE CHAIN CHAOS CHARM CHART " +
            "CHASE CHEAP CHEST CHIEF CHILD CHILL CLAIM CLASH CLEAN CLEAR CLIMB CLOCK CLOSE CLOTH " +
            "CLOUD COACH COAST COULD COUCH COUGH COUNT COURT COVER CRAFT CRANE CRASH CRAVE CRAWL " +
            "CREAM CREEK CRISP CROSS CROWD CRUEL CRUMB CRUSH DAILY DANCE DELAY DEPTH DIRTY DODGE " +
            "DOUBT DRAFT DRAIN DREAD DREAM DRESS DRIFT DRILL DRINK DRIVE DROVE DRUNK DWARF DWELL " +
            "EARLY EARTH ELBOW EMPTY ENJOY EQUAL EQUIP ERUPT EXACT EXIST EXTRA FABLE FAITH FALSE " +
            "FANCY FATAL FAULT FEAST FENCE FERRY FIFTY FIGHT FIXED FLAME FLASH FLEET FLESH FLING " +
            "FLOCK FLOOD FLOOR FLOUR FORCE FORGE FORTH FOUND FRAME FRANK FREAK FRESH FROST FROWN " +
            "FRUIT FULLY FUNNY FUZZY GHOST GIVEN GLAND GLARE GLEAM GLOBE GOOSE GRACE GRADE GRAIN " +
            "GRAND GRANT GRAPE GRASP GRASS GRAVE GRAZE GREED GREET GRIEF GRIND GROAN GROOM GROWL " +
            "GUARD GUESS GUEST GUIDE GUSTO HABIT HANDY HARSH HATCH HEART HEAVY HEFTY HONOR HORSE " +
            "HOTEL HUMAN HUMOR HUNCH IMAGE IMPLY INDEX INNER IRATE IVORY JELLY JEWEL JUICE JUMBO " +
            "KNACK KNEEL KNIFE KNOCK KNOWN LABEL LANCE LARGE LASER LAUGH LAYER LEDGE LEGAL LEMON " +
            "LEVEL LIGHT LINER LIVER LOCAL LODGE LOOSE LOUSY LOVER LOYAL LUCKY LUSTY MAGIC MAJOR " +
            "MANOR MAPLE MARCH MARRY MAYOR MEDAL MERCY MERIT MERRY MESSY MIGHT MINOR MISER MOIST " +
            "MONEY MONTH MORAL MOURN MOUTH MURKY MUSTY NASTY NAVAL NERVE NEVER NOBLE NIGHT NOISY " +
            "NOVEL NURSE OCCUR OCEAN OFFER OLIVE ORDER OUGHT OUTER PAINT PANIC PAPER PARTY PAUSE " +
            "PEACE PENNY PHASE PIANO PIECE PINCH PITCH PIZZA PLAIN PLANT PLEAD PLUCK POINT PORCH " +
            "POUND POWER PRANK PRESS PRICE PRIDE PRIME PRINT PRIZE PROOF PROSE PROUD PROVE PULSE " +
            "PUNCH PURSE QUEEN QUERY QUICK QUIET QUIRK QUOTA QUOTE RADAR RADIO RAINY RAISE RALLY " +
            "RANGE RAPID RAVEN RAZOR REACH REALM REBEL REIGN RELAX REPAY RESET RISKY RIVAL RIVER " +
            "ROBOT ROCKY ROUGH ROUND ROUTE ROWDY ROYAL RULER SADLY SAINT SALAD SAUCE SAVVY SCARY " +
            "SCENE SCORE SCOUT SCREW SENSE SERVE SETUP SEVEN SHARE SHARK SHARP SHELF SHELL SHIFT " +
            "SHIRT SHOCK SHONE SHOOK SHOOT SHORE SHORT SHOUT SHOVE SIEGE SINCE SIXTH SKILL SKULL " +
            "SLACK SLASH SLAVE SLEEK SLEEP SLICE SLICK SLIDE SLOPE SMART SMASH SMELL SMILE SMOKE " +
            "SNAIL SNARE SNEAK SOLID SORRY SPARK SPAWN SPEAR SPEED SPELL SPICE SPILL SPINE SPITE " +
            "SPLIT SPOKE SPOOK SPORT SPRAY STACK STAFF STAIN STALE STALK STAMP STAND STARK STATE " +
            "STEAL STEAM STEEL STEEP STEER STERN STIFF STILL STING STOMP STONE STOOD STORE STORM " +
            "STORY STOUT STOVE STRAP STRAW STRAY STRIP STUCK STUDY STUFF STUMP STYLE SUGAR SUITE " +
            "SUNNY SURGE SWEAR SWEAT SWEEP SWEET SWORD SYRUP TALON TASTY TAUNT TENSE THANK THICK " +
            "THIEF THREW THREE THROW TIGER TIGHT TITAN TITLE TOUCH TOUGH TOWER TOXIC TRACE TRACK " +
            "TRADE TRAIL TRASH TREAD TREAT TREND TRIAL TRICK TROUT TRUCK TRULY TRUNK TRUST TRUTH " +
            "TWEAK TWICE TWIST ULCER ULTRA UNCLE UNDER UNION UNTIL UPPER UPSET URBAN USHER USUAL " +
            "UTTER VAGUE VALUE VALVE VAPOR VAULT VIDEO VIGOR VIRAL VIRUS VISIT VISTA VITAL VIVID " +
            "VOTER WAIST WATCH WATER WEAVE WEIGH WEIRD WHALE WHEAT WHEEL WHILE WHOLE WHOSE WIELD " +
            "WITCH WORLD WORRY WORST WORTH WOULD WOUND WRATH WRONG WROTE YACHT YEARN YOUNG ZEBRA " +
            "ABSENT ACCEPT ACCESS ACTION ACTIVE ACTUAL AFFECT AFRAID AGENCY ALMOST ALWAYS AMOUNT " +
            "ANIMAL ANNUAL ANSWER APPEAL APPEAR AROUND ARRIVE ATTACK ATTEND AUTHOR AUTUMN BOTHER " +
            "BOTTLE BOTTOM BRANCH BREATH BRIDGE BRIGHT BROKEN BRONZE BUNDLE BUTTER CANCEL CANYON " +
            "CARBON CAREER CASTLE CATTLE CHANCE CHANGE CHARGE CHOOSE CHOSEN CHURCH CIRCLE CLIENT " +
            "COFFEE COLUMN COMEDY COMMON CORNER COTTON CREATE CREDIT CRISIS CRUISE CUSTOM DAMAGE " +
            "DANCER DANGER DEBATE DECIDE DEFEND DEFINE DEMAND DESIRE DETAIL DETECT DIFFER DIRECT " +
            "DIVIDE DOLLAR DOMAIN DONATE DOUBLE DRIVEN EASILY EFFECT EFFORT EITHER ELEVEN EMPIRE " +
            "ENABLE ENERGY ENGAGE ENOUGH ENSURE ENTIRE ESCAPE ESTATE EXCEPT EXPECT EXPERT EXTEND " +
            "FABRIC FACTOR FALLEN FAMOUS FATHER FINGER FINISH FLAVOR FLIGHT FORMAL FOSSIL FRIEND " +
            "FROZEN FUTURE GARAGE GENDER GENTLE GOLDEN GOVERN GROUND GROWTH GUILTY HANDLE HAPPEN " +
            "HEALTH HEIGHT HONEST HUNTER IMPACT IMPORT INCOME INSIDE INVEST ISLAND JUNGLE JUNIOR " +
            "LAUNCH LEADER LEGEND LENGTH LESSON LETTER LIKELY LISTEN LITTLE LIVING LOVELY MANNER " +
            "MARBLE MARGIN MARKET MASTER MATTER MEADOW MENTAL METHOD MIDDLE MINUTE MIRROR MOBILE " +
            "MODERN MOMENT MOTION MOTIVE MURDER MUSCLE MUTUAL NARROW NATION NATURE NEARBY NEEDLE " +
            "NORMAL NOTICE NUMBER OBJECT OBTAIN OFFICE ONLINE OPTION ORANGE ORIGIN PLAYER PLEASE " +
            "PLENTY POCKET POETRY POLICE POLICY PONDER PORTAL PREFER PRETTY PRINCE PROFIT PROMPT " +
            "PUBLIC PURSUE RABBIT RACIAL RANDOM REASON RECORD REDUCE REFORM REFUSE REGION REMAIN " +
            "REMOVE REPEAT RESCUE RETURN REVEAL REWARD RIBBON ROCKET RUBBER SAFETY SAMPLE SCHOOL " +
            "SEARCH SECOND SECRET SECURE SELECT SERVER SETTLE SEVERE SHADOW SIMPLE SISTER SINGLE " +
            "SKETCH SOCIAL SOURCE STABLE STATUE STATUS STEADY STICKY STREAM STREET STRICT STRONG " +
            "STUPID SUBMIT SUPPLY SWITCH SYSTEM TACKLE TALENT TARGET TEMPLE THEORY TISSUE TONGUE " +
            "TRAVEL TREATY TRIPLE TROPHY TUNNEL TURTLE TWELVE TWENTY UNIQUE UPDATE USEFUL VALLEY " +
            "VARIED VENDOR VERSUS VICTIM VISUAL VOLUME VOYAGE WALLET WANDER WEAPON WEIGHT WINDOW " +
            "WISDOM WONDER WORTHY YELLOW";
        for (String w : raw.split(" ")) if (!w.isEmpty()) words.add(w);
    }
}

class WordFinder {
    private final Board board;
    WordFinder(Board b) { board=b; }
    List<Square> findHorizontalWord(int row, int col) {
        int s=col; while(s>0&&!board.getSquare(row,s-1).isEmpty()) s--;
        int e=col; while(e<Board.SIZE-1&&!board.getSquare(row,e+1).isEmpty()) e++;
        List<Square> w=new ArrayList<>(); for(int c=s;c<=e;c++) w.add(board.getSquare(row,c)); return w;
    }
    List<Square> findVerticalWord(int row, int col) {
        int s=row; while(s>0&&!board.getSquare(s-1,col).isEmpty()) s--;
        int e=row; while(e<Board.SIZE-1&&!board.getSquare(e+1,col).isEmpty()) e++;
        List<Square> w=new ArrayList<>(); for(int r=s;r<=e;r++) w.add(board.getSquare(r,col)); return w;
    }
    String wordToString(List<Square> sq) {
        StringBuilder sb=new StringBuilder(); sq.forEach(s->sb.append(s.getTile().getLetter())); return sb.toString();
    }
    void validateWord(List<Square> sq, Dictionary d) throws InvalidWordException {
        if (sq.size()<2) return;
        String w=wordToString(sq);
        if (!d.isValidWord(w)) throw new InvalidWordException("\""+w+"\" is not a valid Scrabble word.");
    }
}

class ScoreCalculator {
    ScoreBreakdown calculate(List<Square> word, List<Square> newlyPlaced) {
        ScoreBreakdown bd=new ScoreBreakdown(); int wm=1, raw=0;
        for (Square sq:word) {
            int ls=sq.getTile().getValue();
            if (newlyPlaced.contains(sq)) { ls*=sq.getLetterMultiplier(); wm*=sq.getWordMultiplier(); }
            raw+=ls; bd.addLetter(sq.getTile().getLetter(), ls);
        }
        bd.setWordMult(wm); bd.setFinal(raw*wm); return bd;
    }
    int sumScores(List<ScoreBreakdown> bds) {
        return bds.stream().mapToInt(ScoreBreakdown::getFinal).sum();
    }

    static class ScoreBreakdown {
        private final StringBuilder detail=new StringBuilder();
        private int wm=1, total;
        void addLetter(char l, int p) { detail.append(l).append('(').append(p).append(')'); }
        void setWordMult(int m)       { wm=m; }
        void setFinal(int t)          { total=t; }
        int  getFinal()               { return total; }
        @Override public String toString() { return detail+" x"+wm+"="+total; }
    }
}

final class GameState implements Serializable {
    private final Board board; private final List<Player> players;
    private final TileBag tileBag; private final int currentPlayerIndex;
    GameState(Board b, List<Player> p, TileBag t, int i) { board=b; players=p; tileBag=t; currentPlayerIndex=i; }
    Board        getBoard()              { return board; }
    List<Player> getPlayers()            { return players; }
    TileBag      getTileBag()            { return tileBag; }
    int          getCurrentPlayerIndex() { return currentPlayerIndex; }
}

public class ScrabbleGame {

    private Board board; private TileBag tileBag; private Dictionary dictionary;
    private List<Player> players; private int currentPlayerIndex;
    private WordFinder wordFinder; private ScoreCalculator scoreCalc;
    private List<Square> pendingSquares = new ArrayList<>();
    private Tile selectedTile; private Button selectedRackBtn;

    private final Map<String,Integer>      wordsPlayed = new HashMap<>();
    private final WordHistory              wordHistory  = new WordHistory();
    private final List<GameRecord<Player>> gameHistory  = new ArrayList<>();

    private Button[][] boardBtns; private HBox rackBox;
    private Label statusLbl, turnLbl, tilesLbl;
    private Label[] scoreLbls;
    private Stage primaryStage;

    public void start(Stage stage) {
        this.primaryStage = stage;
        initModel();
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");
        root.setPadding(new Insets(10));
        root.setTop(buildTop());
        root.setCenter(buildBoard());
        root.setBottom(buildBottom());
        root.setRight(buildSide());
        Scene scene = new Scene(root, 1100, 800);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        stage.setTitle("SmartScrabble – Sabir Ali 73971");
        stage.setScene(scene); stage.show();
        refresh();
    }

    private void initModel() {
        board=new Board(); tileBag=new TileBag(); dictionary=new Dictionary();
        wordFinder=new WordFinder(board); scoreCalc=new ScoreCalculator();
        players=new ArrayList<>();
        Player p1=new Player("Player 1"), p2=new Player("Player 2");
        p1.addTilesToRack(tileBag.draw(7)); p2.addTilesToRack(tileBag.draw(7));
        players.add(p1); players.add(p2); currentPlayerIndex=0;
    }

    private Player cur() { return players.get(currentPlayerIndex); }

    private VBox buildTop() {
        Label title=lbl("SmartScrabble",26,true,"#e8e8e8");
        turnLbl  = lbl("",16,true,"#5dd6c0");
        statusLbl= lbl("Select a tile from your rack, then click a board square.",12,false,"#cfcfcf");
        statusLbl.setWrapText(true);
        VBox v=new VBox(4,title,turnLbl,statusLbl); v.setPadding(new Insets(0,0,10,0)); return v;
    }

    private GridPane buildBoard() {
        GridPane g=new GridPane(); g.setAlignment(Pos.CENTER); g.setHgap(2); g.setVgap(2);
        boardBtns=new Button[Board.SIZE][Board.SIZE];
        for (int r=0;r<Board.SIZE;r++) for (int c=0;c<Board.SIZE;c++) {
            Button b=new Button(); b.setPrefSize(40,40);
            b.setFont(Font.font("Arial",FontWeight.BOLD,11));
            final int row=r, col=c;
            b.setOnAction(e->onSquareClicked(row,col));
            boardBtns[r][c]=b; g.add(b,c,r);
        }
        return g;
    }

    private VBox buildBottom() {
        rackBox=new HBox(6); rackBox.setAlignment(Pos.CENTER); rackBox.setPadding(new Insets(8));

        Button submitBtn = new Button("Submit Word");
        submitBtn.setOnAction(e -> onSubmitWord());

        Button closeBtn = new Button("Close Game");
        closeBtn.setOnAction(e -> primaryStage.close());

        Button passBtn = new Button("Pass Turn");
        passBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) { onPassTurn(); }
        });

        Button recallBtn=new Button("Recall"); recallBtn.setOnAction(e->onRecall());
        Button saveBtn  =new Button("Save");   saveBtn.setOnAction(e->onSave());
        Button loadBtn  =new Button("Load");   loadBtn.setOnAction(e->onLoad());

        HBox ctrl=new HBox(8,submitBtn,closeBtn,passBtn,recallBtn,saveBtn,loadBtn);
        ctrl.setAlignment(Pos.CENTER);
        Label rl=lbl("Your Rack:",13,false,"#e8e8e8");
        VBox v=new VBox(6,rl,rackBox,ctrl); v.setAlignment(Pos.CENTER); v.setPadding(new Insets(8,0,0,0));
        return v;
    }

    private VBox buildSide() {
        scoreLbls=new Label[2];
        VBox box=new VBox(5); box.getChildren().add(lbl("Scores",16,true,"#e8e8e8"));
        for (int i=0;i<2;i++) { scoreLbls[i]=lbl("",13,false,"#cfcfcf"); box.getChildren().add(scoreLbls[i]); }
        tilesLbl=lbl("",12,false,"#9aa0a6");
        box.getChildren().addAll(new Separator(),tilesLbl);
        box.setPadding(new Insets(0,0,0,12)); box.setPrefWidth(170); return box;
    }

    private static Label lbl(String t,int sz,boolean bold,String hex) {
        Label l=new Label(t);
        l.setFont(Font.font("Arial", bold?FontWeight.BOLD:FontWeight.NORMAL, sz));
        l.setTextFill(Color.web(hex)); return l;
    }

    private void onSquareClicked(int r, int c) {
        Square sq=board.getSquare(r,c);
        if (!sq.isEmpty())      { status("That square is already occupied."); return; }
        if (selectedTile==null) { status("Select a rack tile first.");        return; }
        sq.placeTile(selectedTile); pendingSquares.add(sq);
        cur().removeFromRack(selectedTile); selectedTile=null;
        if (selectedRackBtn!=null) { selectedRackBtn.getStyleClass().remove("rack-tile-selected"); selectedRackBtn=null; }
        refresh();
    }

    private void onRackTileClicked(Tile tile, Button btn) {
        if (selectedRackBtn!=null) selectedRackBtn.getStyleClass().remove("rack-tile-selected");
        selectedTile=tile; selectedRackBtn=btn;
        btn.getStyleClass().add("rack-tile-selected");
        status("Selected '"+tile.getLetter()+"' – click a square.");
    }

    private void onSubmitWord() {
        if (pendingSquares.isEmpty()) { status("Place at least one tile first."); return; }
        try {
            List<List<Square>> allWords=collectWords();
            for (List<Square> w:allWords) wordFinder.validateWord(w,dictionary);

            List<ScoreCalculator.ScoreBreakdown> bds=new ArrayList<>();
            for (List<Square> w:allWords) { if(w.size()<2) continue; bds.add(scoreCalc.calculate(w,pendingSquares)); }

            int pts=scoreCalc.sumScores(bds);
            cur().addScore(pts);

            for (List<Square> w:allWords) {
                String ws=wordFinder.wordToString(w);
                wordsPlayed.merge(ws,1,Integer::sum);
                wordHistory.add(ws);
            }
            gameHistory.add(new GameRecord<>(cur().getName(), cur()));

            status(cur().getName()+" scored "+pts+" pts! Last word: "+wordHistory.latest());
            refillAndAdvance();

        } catch (InvalidWordException e) {
            undoPlacement(); status("Invalid: "+e.getMessage());
        }
        refresh();
    }

    private List<List<Square>> collectWords() {
        List<List<Square>> ws=new ArrayList<>();
        boolean sameRow=pendingSquares.stream().map(Square::getRow).distinct().count()==1;
        if (sameRow) {
            ws.add(wordFinder.findHorizontalWord(pendingSquares.get(0).getRow(), pendingSquares.get(0).getCol()));
            for (Square sq:pendingSquares) { List<Square> v=wordFinder.findVerticalWord(sq.getRow(),sq.getCol()); if(v.size()>1) ws.add(v); }
        } else {
            ws.add(wordFinder.findVerticalWord(pendingSquares.get(0).getRow(), pendingSquares.get(0).getCol()));
            for (Square sq:pendingSquares) { List<Square> h=wordFinder.findHorizontalWord(sq.getRow(),sq.getCol()); if(h.size()>1) ws.add(h); }
        }
        return ws;
    }

    private void undoPlacement() {
        List<Tile> ret=new ArrayList<>();
        for (Square sq:pendingSquares) { ret.add(sq.getTile()); sq.placeTile(null); }
        cur().addTilesToRack(ret); pendingSquares.clear();
    }

    private void onRecall()   { undoPlacement(); status("Tiles recalled."); refresh(); }
    private void onPassTurn() { undoPlacement(); status(cur().getName()+" passed."); advance(); refresh(); }

    private void refillAndAdvance() {
        int need=7-cur().getRack().size();
        if (need>0) cur().addTilesToRack(tileBag.draw(need));
        pendingSquares.clear(); advance();
    }
    private void advance() { currentPlayerIndex=(currentPlayerIndex+1)%players.size(); }

    private void onSave() {
        ObjectOutputStream out=null;
        try {
            out=new ObjectOutputStream(new FileOutputStream("scrabble_save.dat"));
            out.writeObject(new GameState(board,players,tileBag,currentPlayerIndex));
            status("Game saved.");
        } catch (IOException e) {
            status("Save failed: "+e.getMessage());
        } finally {
            if (out!=null) try { out.close(); } catch (IOException ignored) {}
        }
    }

    private void onLoad() {
        try (ObjectInputStream in=new ObjectInputStream(new FileInputStream("scrabble_save.dat"))) {
            GameState gs=(GameState) in.readObject();
            board=gs.getBoard(); players=gs.getPlayers();
            tileBag=gs.getTileBag(); currentPlayerIndex=gs.getCurrentPlayerIndex();
            wordFinder=new WordFinder(board); pendingSquares.clear();
            status("Game loaded."); refresh();
        } catch (IOException|ClassNotFoundException e) {
            status("Load failed: "+e.getMessage());
        }
    }

    private void status(String m) { statusLbl.setText(m); }

    private void refresh() {
        refreshBoard(); refreshRack(); refreshScores();
        turnLbl.setText("Turn: "+cur().getName());
        tilesLbl.setText("Bag: "+tileBag.remaining()+" tiles | Words: "+wordHistory.size());
    }

    private void refreshBoard() {
        for (int r=0;r<Board.SIZE;r++) for (int c=0;c<Board.SIZE;c++) {
            Square sq=board.getSquare(r,c); Button btn=boardBtns[r][c];
            if (!sq.isEmpty()) { btn.setText(String.valueOf(sq.getTile().getLetter())); btn.getStyleClass().setAll("board-tile"); }
            else               { btn.setText(sq.getLabel()); btn.getStyleClass().setAll(cssCls(sq)); }
        }
    }

    private String cssCls(Square sq) {
        if (sq instanceof PremiumSquare ps) return switch(ps.getBonusType()) {
            case TRIPLE_WORD->"board-square-tw"; case DOUBLE_WORD->"board-square-dw";
            case TRIPLE_LETTER->"board-square-tl"; case DOUBLE_LETTER->"board-square-dl";
            case CENTER_STAR->"board-square-center";
        };
        return "board-square";
    }

    private void refreshRack() {
        rackBox.getChildren().clear();
        for (Tile t:cur().getRack()) {
            Button b=new Button(t.getLetter()+" "+t.getValue());
            b.setPrefSize(50,50); b.getStyleClass().add("rack-tile");
            b.setOnAction(e->onRackTileClicked(t,b));
            rackBox.getChildren().add(b);
        }
    }

    private void refreshScores() {
        List<Player> sorted=new ArrayList<>(players);
        sorted.sort((a,b)->b.getScore()-a.getScore());
        for (int i=0;i<sorted.size()&&i<scoreLbls.length;i++)
            scoreLbls[i].setText(sorted.get(i).getName()+": "+sorted.get(i).getScore());
    }
}
