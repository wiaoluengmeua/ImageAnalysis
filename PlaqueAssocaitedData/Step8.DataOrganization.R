library(tidyverse)
library(stringr)

# Load the data
setwd("/research/labs/moleneurosci/bug/m198507/Projects/APOEHetero-Kai/IF_PLIN2")
Dat <- read.csv("measurements.csv")

# Extract the base Image name
Dat$Image <- str_split_fixed(Dat$Image, "_", n = 2)[, 1]

# Define the suffixes for different diameters
suffixes <- c("+0µm", "+10µm", "+15µm", "+20µm", "+25µm", "+30µm")

# Process each suffix
result_list <- lapply(suffixes, function(suffix) {
  # Subset data for the specific suffix
  Dat_subset <- subset(Dat, Name %in% c(
    paste0("Child_Circle", suffix),
    paste0("Child_Circle", suffix, " ∩ Annotation"),
    paste0("Child_Circle", suffix, " ∩ Opal 520"),
    paste0("Child_Circle", suffix, " ∩ Opal 620 ∩ Opal 520")
  ))
  
  # Add the Diameter column
  Dat_subset$Diameter <- paste0("R_", gsub("[^0-9]", "", suffix))
  
  # Rename the `Name` column values
  Dat_subset <- Dat_subset %>%
    mutate(Name = case_when(
      Name == paste0("Child_Circle", suffix) ~ "PlaqueRegion",
      Name == paste0("Child_Circle", suffix, " ∩ Annotation") ~ "PlaqueAssociatedMicroglia",
      Name == paste0("Child_Circle", suffix, " ∩ Opal 520") ~ "PlaqueAssociatedPlin2",
      Name == paste0("Child_Circle", suffix, " ∩ Opal 620 ∩ Opal 520") ~ "PlaqueAssociatedMicroglialPlin2"
    ))
  
  # Assign PlaqueID
  Dat_subset$PlaqueID <- (seq_along(Dat_subset$Object.ID) - 1) %/% 4 + 1
  
  # Ensure Area is numeric
  Dat_subset$Area.µm.2 <- as.numeric(Dat_subset$Area.µm.2)
  
  # Pivot the data to wide format
  Dat_subset %>%
    pivot_wider(
      id_cols = c("Image", "PlaqueID", "Diameter"),
      names_from = "Name",
      values_from = "Area.µm.2"
    )
})

# Combine all results into a single data frame
final_result <- bind_rows(result_list)

write.csv(final_result, "PlaqueAssociatedData.csv", row.names = FALSE)