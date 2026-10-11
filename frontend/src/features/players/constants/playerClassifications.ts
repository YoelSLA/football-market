import bundesligaImage from "../assets/Bundesliga-League.png";
import defenceImage from "../assets/Defence-Player.png";
import goalkeeperImage from "../assets/Goalkeeper-Player.png";
import ligueOneImage from "../assets/Ligue 1-League.png";
import midfieldImage from "../assets/Midfield-Player.png";
import offenceImage from "../assets/Offence-Player.png";
import premierLeagueImage from "../assets/Premier League-League.png";
import primeraDivisionImage from "../assets/Primera Division-League.png";
import serieAImage from "../assets/Serie A-League.png";

interface Classification {
	image: string | null;
	variant: string;
}

const leagueClassifications: Record<string, Classification> = {
	"Premier League": { image: premierLeagueImage, variant: "premier" },
	Bundesliga: { image: bundesligaImage, variant: "bundesliga" },
	"Primera División": { image: primeraDivisionImage, variant: "primera" },
	"Primera Division": { image: primeraDivisionImage, variant: "primera" },
	"Serie A": { image: serieAImage, variant: "serie-a" },
	"Ligue 1": { image: ligueOneImage, variant: "ligue-one" },
};

const positionClassifications: Record<string, Classification> = {
	Goalkeeper: { image: goalkeeperImage, variant: "goalkeeper" },
	Defence: { image: defenceImage, variant: "defence" },
	Midfield: { image: midfieldImage, variant: "midfield" },
	Offence: { image: offenceImage, variant: "offence" },
};

const neutralClassification: Classification = {
	image: null,
	variant: "neutral",
};

export function getLeagueClassification(leagueName: string): Classification {
	return leagueClassifications[leagueName] ?? neutralClassification;
}

export function getPositionClassification(position: string): Classification {
	return positionClassifications[position] ?? neutralClassification;
}
