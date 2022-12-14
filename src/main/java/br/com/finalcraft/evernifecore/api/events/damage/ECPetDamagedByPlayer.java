package br.com.finalcraft.evernifecore.api.events.damage;

import br.com.finalcraft.evernifecore.config.playerdata.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * This event is fired when a player causes damage to a pet!
 * The damage can come from thid-part sources like a Projectile.
 *
 * @author EverNife
 */
public class ECPetDamagedByPlayer extends ECPlayerdataDamagePlayerdata {

    private final Tameable tamableVictim;

    public ECPetDamagedByPlayer(PlayerData attackerData, PlayerData victimData, Tameable tamableVictim, EntityDamageByEntityEvent entityDamageByEntityEvent) {
        super(attackerData, victimData, entityDamageByEntityEvent);
        this.tamableVictim = tamableVictim;
    }

    public Player getAttacker() {
        return (Player) entityDamageByEntityEvent.getDamager();
    }

    public Tameable getVictim() {
        return tamableVictim;
    }

}
