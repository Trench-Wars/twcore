package twcore.bots.multibot.util;

import twcore.bots.MultiUtil;
import twcore.core.BotAction;
import twcore.core.events.Message;
import twcore.core.util.ModuleEventRequester;

/**
    Utility set designed to manage, and modify components of arenas that
    otherwise need manual editing by a System Operator.

    @author JabJabJab

*/
public class utildev extends MultiUtil {

    /**
        List of arenas to be blacklisted from modification for control purposes.
    */
    public String[] arenaBlackList = new String[] { "elim", "baseelim", "duel",
            "tourny", "wbduel", "javduel", "spidduel", "base"
                                                  };

    @Override
    public void init() {
    }

    @Override
    public String[] getHelpMessages() {
        String help[] = {
            "==Dev Utilities==",
            "!setResolution <x> <y> (sets the resolution cap)",
            "!setResolution <preset> (Presets: twd, twl, clear)"
        };
        return help;
    }

    @Override
    public void requestEvents(ModuleEventRequester modEventReq) {

    }

    public void handleEvent(Message event) {
        String message = event.getMessage();

        if (event.getMessageType() == Message.PRIVATE_MESSAGE) {
            String name = m_botAction.getPlayerName(event.getPlayerID());
            handleCommand(name, message);
        }
    }

    public void handleCommand(String player, String message) {
        BotAction botAction;

        String[] parameters;

        String arenaName;

        String command;

        boolean validArenaForResolutionModifications;

        botAction = getBotAction();

        botAction.sendPublicMessage("test");

        arenaName = botAction.getArenaName();

        command = message.toLowerCase();

        validArenaForResolutionModifications = isValidArenaForResolutionModification(arenaName);

        parameters = getParameters(command);

        if (m_opList.isSmod(player)) {
            if (command.startsWith("!setresolution")) {
                if (validArenaForResolutionModifications) {
                    handleResolutionChange(player, parameters);
                } else {
                    botAction.sendPrivateMessage(player,
                                                 "Invalid arena for command. (BlackListed)");
                }
            }
        }

    }

    public boolean isValidArenaForResolutionModification(String arenaName) {
        String arena = arenaName.toLowerCase();

        if (arena.contains("public")) {
            return false;
        }

        if (arena.contains("twdd") || arena.contains("twld")
                || arena.contains("twbd") || arena.contains("twsd")) {
            return false;
        }

        for (String string : arenaBlackList) {
            if (string.toLowerCase().equals(arena)) {
                return false;
            }
        }

        return true;
    }

    // TWD : 1440 1024
    // TWL : 1440 900
    private void handleResolutionChange(String player, String[] parameters) {

        BotAction botAction = getBotAction();

        int resolutionX = 0;
        int resolutionY = 0;

        boolean clear = false;

        if (parameters.length >= 1) {
            if (parameters[0] == null) {
                botAction.sendPrivateMessage(player,
                                             "Invalid parameters. !setResolution x y");
                return;
            }

            if (parameters[0].equals("twd")) {
                resolutionX = 1440;
                resolutionY = 1024;
            } else if (parameters[0].equals("twl")) {
                resolutionX = 1440;
                resolutionY = 900;
            } else if (parameters[0].equals("empty")
                       || parameters[0].equals("none")
                       || parameters[0].equals("off")) {
                clear = true;
            } else {
                // If the parameters are at least 2 (ignoring anything else
                // sent)
                if (parameters.length >= 2) {
                    try {
                        // Grab our value parameters.
                        resolutionX = Integer.parseInt(parameters[0]);
                        resolutionY = Integer.parseInt(parameters[1]);

                        if (resolutionX < 1024 || resolutionY < 768) {
                            // Check if numbers are positive.
                            if (resolutionX < 1 || resolutionY < 1) {
                                botAction
                                .sendPrivateMessage(
                                    player,
                                    "Invalid parameters. Values must be positive, and greater than or equal to 800 600.");
                                return;
                            }

                            botAction
                            .sendPrivateMessage(player,
                                                "Invalid parameters. Values must be at least 800 600");
                            return;
                        }
                    } catch (NumberFormatException e) {
                        botAction.sendPrivateMessage(player,
                                                     "Invalid parameters. !setResolution x y");
                        return;
                    }
                }
            }

            if (clear) {
                botAction.sendUnfilteredPublicMessage("?set Misc:MaxXRes= ");
                botAction.sendUnfilteredPublicMessage("?set Misc:MaxYRes= ");
                botAction.sendPrivateMessage(player, "Resolution cleared.");

            } else {
                botAction.sendUnfilteredPublicMessage("?set Misc:MaxXRes="
                                                      + resolutionX);
                botAction.sendUnfilteredPublicMessage("?set Misc:MaxYRes="
                                                      + resolutionY);
                botAction.sendPrivateMessage(player, "Resolution set.");
            }

        } else {
            botAction.sendPrivateMessage(player,
                                         "E.G: !setResolution 1280 1024 ; !setResolution twd");
        }
    }

    private String[] getParameters(String command) {
        String[] split;
        String[] parameters;
        int validCount = 0;
        int count = 0;

        split = command.split(" ");

        for (int index = 0; index < split.length; index++) {
            String splitString = split[index];

            if (splitString != null && !splitString.isEmpty()) {
                validCount++;
            } else {
                split[index] = null;
            }
        }

        parameters = new String[validCount];

        for (int index = 1; index < split.length; index++) {
            String parameter = split[index];

            if (parameter != null) {
                parameters[count++] = split[index];
            }
        }

        return parameters;
    }

    public BotAction getBotAction() {
        return this.m_botAction;
    }
}
