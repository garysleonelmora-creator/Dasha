DASHA - APP ANDROID

Esta versión usa exactamente PORTADA_DASHA_ORIGINAL.png como pantalla de presentación.

FUNCIONAMIENTO:
1. Abre el proyecto DashaAndroid en Android Studio.
2. Ejecuta la app en un teléfono Android.
3. Al iniciar verás la portada Dasha.
4. Después la app te pedirá la URL del servidor Dasha.
5. En la misma Wi-Fi puedes usar:
   http://IP-DE-TU-PC:8765/
6. Para usarla desde cualquier lugar y publicarla en Google Play necesitas una URL HTTPS pública para el servidor.

La API Key de OpenAI NO se guarda dentro de la app Android.
Se mantiene en el servidor Dasha, donde el administrador también crea,
bloquea, reactiva y limita usuarios.

Para generar un APK:
Build > Build Bundle(s) / APK(s) > Build APK(s)

Para Google Play:
Build > Generate Signed Bundle / APK > Android App Bundle (.aab)
