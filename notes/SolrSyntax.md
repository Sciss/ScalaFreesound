# Logical operators

- `AND`, `OR`, grouping via parenthesis `(`, `)`.
- logical NOT is tricky. `NOT+2` doesn't work.
  Instead one has to write `(*:*+NOT+2)`
- inclusive ranges: `[<start> TO <stop>]` where
  either start and stop can be the wildcard `*`,
  e.g. `[* TO 10]` means less than or equal to ten.
  
# Response fields

## default

id,name,tags,license,username

## always we want

- id 	number 	The sound’s unique identifier.
- name 	string 	The name user gave to the sound.
- tags 	array[strings] 	An array of tags the user gave to the sound.
- description 	string 	The description the user gave to the sound.
- geotag 	string 	Latitude and longitude of the geotag separated by spaces (e.g. “41.0082325664 28.9731252193”, only for sounds that have been geotagged).
- created 	string 	The date when the sound was uploaded (e.g. “2014-04-16T20:07:11.145”).
- license 	string 	The license under which the sound is available to you.
- type 	string 	The type of sound (wav, aif, aiff, mp3, or flac).
- channels 	number 	The number of channels.
- filesize 	number 	The size of the file in bytes.
- bitrate 	number 	The bit rate of the sound in kbps.
- bitdepth 	number 	The bit depth of the sound.
- duration 	number 	The duration of the sound in seconds.
- samplerate 	number 	The samplerate of the sound.
- username 	string 	The username of the uploader of the sound.
- pack 	URI 	If the sound is part of a pack, this URI points to that pack’s API resource.
- images 	object 	Dictionary including the URIs for spectrogram and waveform visualizations of the sound. The dinctionary includes the fields waveform_l and waveform_m (for large and medium waveform images respectively), and spectral_l and spectral_m (for large and medium spectrogram images respectively).
- num_downloads 	number 	The number of times the sound was downloaded.
- avg_rating 	number 	The average rating of the sound.
- num_ratings 	number 	The number of times the sound was rated.
- num_comments 	number 	The number of comments.
- analysis 	object 	Object containing requested descriptors information according to the descriptors request parameter (see below). This field will be null if no descriptors were specified (or invalid descriptor names specified) or if the analysis data for the sound is not available.
- analysis_stats 	URI 	URI pointing to the complete analysis results of the sound (see Analysis Descriptor Documentation).
- analysis_frames 	URI 	The URI for retrieving a JSON file with analysis information for each frame of the sound (see Analysis Descriptor Documentation).

## never we want

(these can be constructed from the id and username)

- url 	URI 	The URI for this sound on the Freesound website.
- bookmark 	URI 	The URI for bookmarking the sound.
- comments 	URI 	The URI of a paginated list of the comments of the sound.
- comment 	URI 	The URI to comment the sound.
- similar_sounds 	URI 	URI pointing to the similarity resource (to get a list of similar sounds).
- rate 	URI 	The URI for rating the sound.
- download 	URI 	The URI for retrieving the original sound.

## optionally we want

- previews 	object 	Dictionary containing the URIs for mp3 and ogg versions of the sound. The dictionary includes the fields preview-hq-mp3 and preview-lq-mp3 (for ~128kbps quality and ~64kbps quality mp3 respectively), and preview-hq-ogg and preview-lq-ogg (for ~192kbps quality and ~80kbps quality ogg respectively). API authentication is required for retrieving sound previews (Token or OAuth2).
