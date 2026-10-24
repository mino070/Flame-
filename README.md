# FlameKatana (Paper 1.21.x)

Ek Minecraft plugin jo ek **Flame Katana** deta hai.

## Kya karta hai
- Katana haath me lekar **F** (swap hands) dabao -> **Flame Slash** nikalta hai.
- Slash **4 heart (8 HP)** damage karta hai aur target ko aag laga deta hai.
- Aag ke **particles** dikhte hain (crescent shape ka flame wave).
- **Raat ko power badh jata hai:**
  - Damage x1.5 (6 heart)
  - Range 10 -> 16 blocks
  - Slash zyada chauda (2.5 -> 3.5)
  - Cooldown 3s -> 1.5s
  - Aag 4s -> 8s
  - Flame neeli (soul fire) ho jati hai

## Install
1. `FlameKatana-1.0.0.jar` ko server ke `plugins/` folder me daalo.
2. Server restart karo.
3. Game me `/katana give` likho (OP chahiye) -> sword mil jayegi.
4. Sword haath me pakdo aur **F** dabao.

Zaroori: **Paper 1.21.x** server aur **Java 21**.
(Spigot par nahi chalega, kyunki Paper ki Adventure API use hui hai.)

## Commands / Permissions
| Command | Kaam | Permission |
|---|---|---|
| `/katana give [player]` | Katana deta hai | `flamekatana.admin` (OP) |
| `/katana reload` | config.yml reload | `flamekatana.admin` (OP) |

`flamekatana.use` (default: sabke liye) - F se slash chalane ki permission.

## Config
`plugins/FlameKatana/config.yml` me damage, cooldown, range, raat ka time, sab badal sakte ho.
Damage half-heart me hota hai: `8.0` = 4 heart.

## Source code se build karna
```
mvn clean package
```
Jar `target/FlameKatana-1.0.0.jar` me milega. (JDK 21 aur Maven chahiye.)

PC pe kuch install nahi karna? Is folder ko apne GitHub repo me daal do.
`.github/workflows/build.yml` apne aap jar bana dega: repo ke **Actions** tab me
build kholo aur neeche **Artifacts -> FlameKatana-jar** se download karo.

## Files
- `FlameKatana.java` - main class
- `KatanaItem.java` - sword banana aur pehchanna
- `KatanaListener.java` - F key (swap hands) event
- `FlameSlash.java` - slash, particles, damage, night power
- `KatanaCommand.java` - `/katana` command
