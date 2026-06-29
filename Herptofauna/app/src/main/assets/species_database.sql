--
-- File generated with SQLiteStudio v3.4.17 on Tue Jun 30 11:11:51 2026
--
-- Text encoding used: System
--
PRAGMA foreign_keys = off;
BEGIN TRANSACTION;

-- Table: species_data
CREATE TABLE IF NOT EXISTS species_data (speciesId INTEGER PRIMARY KEY AUTOINCREMENT, scientificName TEXT, englishName TEXT, speciesComment TEXT, speciesImage TEXT);
INSERT INTO species_data (speciesId, scientificName, englishName, speciesComment, speciesImage) VALUES (1, 'Woodworthia "southern alps"', 'Southern Alps Gecko', NULL, NULL);
INSERT INTO species_data (speciesId, scientificName, englishName, speciesComment, speciesImage) VALUES (2, 'Woodworthia "southern mini"', 'Short-toed Gecko', NULL, NULL);
INSERT INTO species_data (speciesId, scientificName, englishName, speciesComment, speciesImage) VALUES (3, 'Woodworthia "Raggedy Range"', 'Raggedy Range Gecko', NULL, NULL);
INSERT INTO species_data (speciesId, scientificName, englishName, speciesComment, speciesImage) VALUES (4, 'Woodworthia "south-western"', 'Mountain Beech Gecko', NULL, NULL);
INSERT INTO species_data (speciesId, scientificName, englishName, speciesComment, speciesImage) VALUES (5, 'Woodworthia "Otago/Southland large"', 'Korero Gecko', NULL, NULL);
INSERT INTO species_data (speciesId, scientificName, englishName, speciesComment, speciesImage) VALUES (6, 'Woodworthia "Central Otago"', 'Schist Gecko', NULL, NULL);
INSERT INTO species_data (speciesId, scientificName, englishName, speciesComment, speciesImage) VALUES (7, 'Woodwirthia "Cromwell"', 'Kawarau Gecko', NULL, NULL);
INSERT INTO species_data (speciesId, scientificName, englishName, speciesComment, speciesImage) VALUES (8, 'Oligosoma toka', 'Nevis Skink', NULL, NULL);
INSERT INTO species_data (speciesId, scientificName, englishName, speciesComment, speciesImage) VALUES (9, 'Oligosoma repens', 'Eyres Skink', NULL, NULL);
INSERT INTO species_data (speciesId, scientificName, englishName, speciesComment, speciesImage) VALUES (10, 'Oligosoma polychroma', 'New Zealand Grass Skink', NULL, NULL);
INSERT INTO species_data (speciesId, scientificName, englishName, speciesComment, speciesImage) VALUES (11, 'Oligosoma otagense', 'Otago Skink', NULL, NULL);
INSERT INTO species_data (speciesId, scientificName, englishName, speciesComment, speciesImage) VALUES (12, 'Oligosoma maccanni', 'McCann''s Skink', NULL, NULL);
INSERT INTO species_data (speciesId, scientificName, englishName, speciesComment, speciesImage) VALUES (13, 'Oligosoma judgei', 'Barrier Skink', NULL, NULL);
INSERT INTO species_data (speciesId, scientificName, englishName, speciesComment, speciesImage) VALUES (14, 'Oligosoma inconspicuum', 'Cyptic Skink', NULL, NULL);
INSERT INTO species_data (speciesId, scientificName, englishName, speciesComment, speciesImage) VALUES (15, 'Oligosoma grande', 'Grand Skink', NULL, NULL);

COMMIT TRANSACTION;
PRAGMA foreign_keys = on;
