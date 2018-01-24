package basemod;

import basemod.helpers.*;
import basemod.interfaces.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.*;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.*;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.ui.panels.EnergyPanel;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndObtainEffect;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class DevConsole implements PostEnergyRechargeSubscriber, PostInitializeSubscriber, PostRenderSubscriber, PostUpdateSubscriber {    
    public static final Logger logger = LogManager.getLogger(DevConsole.class.getName());
    
    private static final float CONSOLE_X = 75.0f;
    private static final float CONSOLE_Y = 75.0f;
    private static final float CONSOLE_W = 800.0f;
    private static final float CONSOLE_H = 40.0f;
    private static final float CONSOLE_PAD_X = 15.0f;
    private static final int CONSOLE_TEXT_SIZE = 30;
    private static final int BACKSPACE_INTERVAL = 4;
    
    private static BitmapFont consoleFont = null;
    private static Color consoleColor = Color.BLACK;
    private static InputProcessor consoleInputProcessor; 
    private static InputProcessor otherInputProcessor = null;
    
    private static boolean infiniteEnergy = false;
    
    public static boolean backspace = false;
    public static boolean visible = false;
    public static int backspaceWait = 0;
    public static int toggleKey = Keys.GRAVE;
    public static String currentText = "";
    
    public DevConsole() {
        BaseMod.subscribeToPostEnergyRecharge(this);
        BaseMod.subscribeToPostInitialize(this);
        BaseMod.subscribeToPostRender(this);
        BaseMod.subscribeToPostUpdate(this);
    }
    
    public static void execute() {
        String[] tokens = currentText.split(" ");
        currentText = "";
        
        if (tokens.length < 1) return;
        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = tokens[i].trim();
        }
        
        switch (tokens[0].toLowerCase()) {
            case "relic": {
                cmdRelic(tokens);
                break;
            }
            case "hand": {
                cmdHand(tokens);
                break;
            }
            case "info": {
                Settings.isInfo = !Settings.isInfo;
                break;
            }
            case "kill": {
                cmdKill(tokens);
                break;
            }
            case "gold": {
                cmdGold(tokens);
                break;
            }
            case "energy": {
                cmdEnergy(tokens);
                break;
            }
            case "deck": {
                cmdDeck(tokens);
                break;
            }
            case "draw": {
                cmdDraw(tokens);
                break;
            }
            default: {
                // TODO: Implement command hook
                break;
            }
        }
    }
    
    private static void cmdRelic(String[] tokens) {
        if (AbstractDungeon.player != null) {
            if (tokens.length < 2) {
                return;
            }
            
            if (tokens[1].equals("r") && tokens.length > 2) {
                String[] relicNameArray = Arrays.copyOfRange(tokens, 2, tokens.length);
                String relicName = String.join(" ", relicNameArray);
                AbstractDungeon.player.loseRelic(relicName);
            } else if (tokens[1].equals("add") && tokens.length > 2) {
                String[] relicNameArray = Arrays.copyOfRange(tokens, 2, tokens.length);
                String relicName = String.join(" ", relicNameArray);
                AbstractDungeon.getCurrRoom().spawnRelicAndObtain(Settings.WIDTH / 2, Settings.HEIGHT / 2, RelicLibrary.getRelic(relicName).makeCopy());
            } else if (tokens[1].equals("list")) {
                Collections.sort(RelicLibrary.starterList);
                Collections.sort(RelicLibrary.commonList);
                Collections.sort(RelicLibrary.uncommonList);
                Collections.sort(RelicLibrary.rareList);
                Collections.sort(RelicLibrary.bossList);
                Collections.sort(RelicLibrary.specialList);
                Collections.sort(RelicLibrary.shopList);
                logger.info(RelicLibrary.starterList);
                logger.info(RelicLibrary.commonList);
                logger.info(RelicLibrary.uncommonList);
                logger.info(RelicLibrary.rareList);
                logger.info(RelicLibrary.bossList);
            }
        }
    }
    
    private static void cmdHand(String[] tokens) {
        if (AbstractDungeon.player != null) {
            if (tokens.length < 3) {
                return;
            }
            
            int upgradeIndex = 3;
            while (upgradeIndex < tokens.length && ConvertHelper.tryParseInt(tokens[upgradeIndex], 0) == 0) {
                ++upgradeIndex;
            }
            
            if (tokens[1].equals("add")) {
                String[] cardNameArray = Arrays.copyOfRange(tokens, 2, upgradeIndex);
                String cardName = String.join(" ", cardNameArray);
                AbstractCard c = CardLibrary.getCard(cardName);
                
                if (c != null) {
                    c = c.makeCopy();
                    
                    if (upgradeIndex != tokens.length) {
                        int upgradeCount = ConvertHelper.tryParseInt(tokens[upgradeIndex], 0);
                        for (int i = 0; i < upgradeCount; i++) {
                            c.upgrade();
                        }
                    }
                    
                    AbstractDungeon.actionManager.addToBottom(new MakeTempCardInHandAction(c, true));
                }
            } else if (tokens[1].equals("r")) {
                if (tokens[2].equals("all")) {
                    for (AbstractCard c : new ArrayList<AbstractCard>(AbstractDungeon.player.hand.group)) {
                        AbstractDungeon.player.hand.moveToExhaustPile(c);
                    }
                    return;
                }
                
                String[] cardNameArray = Arrays.copyOfRange(tokens, 2, tokens.length);
                String cardName = String.join(" ", cardNameArray);
                
                boolean removed = false;
                AbstractCard toRemove = null;
                for (AbstractCard c : AbstractDungeon.player.hand.group) {
                    if(removed) break;
                    if(c.cardID.equals(cardName)){
                        toRemove = c;
                        removed = true;
                    }
                }
                if (removed) AbstractDungeon.player.hand.moveToExhaustPile(toRemove);
            }
        }
    }
    
    private static void cmdKill(String[] tokens) {
        if (AbstractDungeon.getCurrRoom().monsters != null) {
            if (tokens.length != 2) {
                return;
            }
            
            if (tokens[1].equals("all")) {
                int monsterCount = AbstractDungeon.getCurrRoom().monsters.monsters.size();
                int[] multiDamage = new int[monsterCount];
                for (int i = 0; i < monsterCount; ++i) {
                    multiDamage[i] = 999;
                }
                
                AbstractDungeon.actionManager.addToBottom(new DamageAllEnemiesAction(AbstractDungeon.player, multiDamage, DamageInfo.DamageType.HP_LOSS, AbstractGameAction.AttackEffect.NONE));
            }
        }
    }
    
    private static void cmdGold(String[] tokens) {
        if (AbstractDungeon.player != null) {
            if (tokens.length != 3) {
                return;
            }
            
            int amount = ConvertHelper.tryParseInt(tokens[2], 0);
            if (tokens[1].equals("add")) {
                AbstractDungeon.player.displayGold += amount;
                AbstractDungeon.player.gainGold(amount);
            } else if (tokens[1].equals("r")) {
                AbstractDungeon.player.displayGold = Math.max(AbstractDungeon.player.displayGold - amount, 0);
                AbstractDungeon.player.loseGold(amount);
            }
        }
    }
    
    private static void cmdEnergy(String[] tokens) {
        if (AbstractDungeon.player != null) {
            if (tokens.length < 2) {
                return;
            }
            
            if (tokens[1].equals("add") && tokens.length > 2) {
                AbstractDungeon.player.gainEnergy(ConvertHelper.tryParseInt(tokens[2], 0));
            } else if (tokens[1].equals("r") && tokens.length > 2) {
                AbstractDungeon.player.loseEnergy(ConvertHelper.tryParseInt(tokens[2], 0));
            } else if (tokens[1].equals("inf")) {
                infiniteEnergy = !infiniteEnergy;
                if (infiniteEnergy) {
                    AbstractDungeon.player.gainEnergy(9999);
                }
            }
        }
    }
    
    private static void cmdDeck(String[] tokens) {
        if (AbstractDungeon.player != null) {
            if (tokens.length < 3) {
                return;
            }
            
            int upgradeIndex = 3;
            while (upgradeIndex < tokens.length && ConvertHelper.tryParseInt(tokens[upgradeIndex], 0) == 0) {
                ++upgradeIndex;
            }

            String[] cardNameArray = Arrays.copyOfRange(tokens, 2, upgradeIndex);
            String cardName = String.join(" ", cardNameArray);
            
            if (tokens[1].equals("add")) {
                AbstractCard c = CardLibrary.getCard(cardName);
                if (c != null) {
                    c = c.makeCopy();
                
                    if (upgradeIndex != tokens.length) {
                        int upgradeCount = ConvertHelper.tryParseInt(tokens[upgradeIndex], 0);
                        for (int i = 0; i < upgradeCount; i++) {
                            c.upgrade();
                        }
                    }
                        
                    AbstractDungeon.effectList.add(new ShowCardAndObtainEffect(c, (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f));
                } else if (tokens[1].equals("r")) {
                    AbstractDungeon.player.masterDeck.removeCard(cardName);
                }
            }
        }
    }
    
    private static void cmdDraw(String[] tokens) {
        if (AbstractDungeon.getCurrRoom().monsters != null) {
            if (tokens.length != 2) {
                return;
            }
            
            AbstractDungeon.actionManager.addToTop(new DrawCardAction(AbstractDungeon.player, ConvertHelper.tryParseInt(tokens[1], 0)));
        }
    }
    
    public void receivePostEnergyRecharge() {
        if (infiniteEnergy) {
            EnergyPanel.setEnergy(9999);
        }
    }
    
    public void receivePostInitialize() {
        consoleInputProcessor = new ConsoleInputProcessor();      
        
        // Console font
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("font/Kreon-Regular.ttf"));
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();
        parameter.size = (int) (CONSOLE_TEXT_SIZE * Settings.scale);
        consoleFont = generator.generateFont(parameter);
        generator.dispose();
    }
    
    public void receivePostRender(SpriteBatch sb) {
        if (visible && consoleFont != null) {       
            // Since we need a shape renderer, need to end then restart the SpriteBatch
            // Should probably just make a background texture for the console so this doesn't need to be done
            sb.end();
            
            ShapeRenderer consoleBackground = new ShapeRenderer();
            consoleBackground.begin(ShapeType.Filled);
            consoleBackground.setColor(consoleColor);
            consoleBackground.rect(CONSOLE_X, CONSOLE_Y, (CONSOLE_W * Settings.scale), (CONSOLE_H * Settings.scale));
            consoleBackground.end();
            
            sb.begin();
            
            float x = (CONSOLE_X + (CONSOLE_PAD_X * Settings.scale));
            float y = (CONSOLE_Y + (float) Math.floor(CONSOLE_TEXT_SIZE * Settings.scale));
            consoleFont.draw(sb, currentText, x, y);
        }
    }
    
    public void receivePostUpdate() {
        --backspaceWait;
        
        if (backspace && currentText.length() > 0) {
            if (backspaceWait <= 0) {
                currentText = currentText.substring(0, currentText.length()-1);
                backspaceWait = BACKSPACE_INTERVAL;
            }
        }
        
        if (Gdx.input.isKeyJustPressed(toggleKey)) {
            if (visible) {
                currentText = "";
            } else {
                otherInputProcessor = Gdx.input.getInputProcessor();
            }
            
            Gdx.input.setInputProcessor(visible ? otherInputProcessor : consoleInputProcessor);
            visible = !visible;
        }
    }
}