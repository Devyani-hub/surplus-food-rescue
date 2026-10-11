# Surplus Food Rescue

A Spring Boot + MySQL web app connecting surplus-food donors with receiving organizations.

## User flow
- `/` — choose Donor or Receiving Organization.
- `/donor.html` — donor profile, create donations, track listings and view reservation notifications.
- `/organization.html` — register an organization, browse available donations, reserve one, and mark its reservation collected.

## Reservation behavior
A reservation conditionally changes a listing from `AVAILABLE` to `MATCHED` in the database. Only one simultaneous request can change that row, and the allocation table's unique `listing_id` adds another safeguard. A donor notification is inserted in the same transaction. The organization that made the reservation sees the listing in its own reservations and can mark it collected.

## Run on Windows
1. Ensure MySQL is running and the `food_rescue_db` database exists.
2. Check `src/main/resources/application.properties` and set your own database username/password locally. Do not commit real credentials.
3. Open this project folder (the one containing `pom.xml` and `mvnw.cmd`) in VS Code.
4. Stop any old copy of the app running on port 8080 with `Ctrl+C` in its terminal.
5. Run a clean build/start so old compiled HTML is not served:

   ```powershell
   .\mvnw.cmd clean spring-boot:run
   ```

6. Open `http://localhost:8080/`.

The `target/` build output is intentionally not included in the ZIP; Maven regenerates it from the source files. Keep your existing database and records; do not drop the database.

## Important demo limitation
This simplified version does not require login. The selected donor/organization profile is stored in browser local storage for the demo, so it is not strong identity verification. Do not expose it as a production service without authentication and server-side identity/ownership enforcement.
