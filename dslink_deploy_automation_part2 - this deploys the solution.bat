cd C:\Program Files\7-Zip
7z x "C:\Users\simon.ball\Downloads\OneDrive - Dimension Data\DSLink\template-dslink-java\build\distributions\http-listener-dslink-0.0.1-SNAPSHOT.zip" -aoa -o"C:\Users\simon.ball\Downloads\OneDrive - Dimension Data\DSLink\template-dslink-java\build\distributions"
Cd "C:\Users\simon.ball\Downloads\OneDrive - Dimension Data\DSLink\template-dslink-java\build\distributions\http-listener-dslink-0.0.1-SNAPSHOT"
CMD /K ".\bin\http-listener-dslink -b https://10.12.126.55:8443/conn"