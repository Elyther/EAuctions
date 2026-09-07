public synchronized List<Auction> search(String search) {

    List<Auction> result = new ArrayList<>();

    if (search == null) {
        return getAuctions();
    }

    String query = search
            .trim()
            .toLowerCase()
            .replace("_", "")
            .replace("-", "")
            .replace(" ", "");

    if (query.isEmpty()) {
        return getAuctions();
    }

    for (Auction auction : auctions) {

        if (auction == null || auction.getItem() == null) {
            continue;
        }

        ItemStack item = auction.getItem();

        if (item.getType().isAir()) {
            continue;
        }

        // Material name
        String material = item.getType()
                .name()
                .toLowerCase()
                .replace("_", "");

        if (material.contains(query)) {
            result.add(auction);
            continue;
        }

        // Display name
        if (item.hasItemMeta()
                && item.getItemMeta() != null
                && item.getItemMeta().hasDisplayName()) {

            String displayName = item.getItemMeta()
                    .getDisplayName();

            if (displayName != null) {

                displayName = removeColorCodes(displayName)
                        .toLowerCase()
                        .replace("_", "")
                        .replace("-", "")
                        .replace(" ", "")
                        .trim();

                if (displayName.contains(query)) {
                    result.add(auction);
                }
            }
        }
    }

    return result;
}
