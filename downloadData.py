import csv
import sys
import requests

stock = sys.argv[1]
fromDate = int(sys.argv[2])
toDate = int(sys.argv[3])

fieldnames = [
    'timestamp',
    'open',
    'high',
    'low',
    'close',
    'adjusted_close',
    'volume',
    'dividend_amount',
    'split_coefficient'
]

params = (
    ('function', 'TIME_SERIES_DAILY_ADJUSTED'),
    ('symbol', stock),
    ('outputsize', 'full'),
    ('apikey', '33BVN95XBGKECYOB'),
    ('datatype', 'csv'),
)

r = requests.get('https://www.alphavantage.co/query/', params=params)
filename = "stockData/"+stock + ".csv"
newfile = "stockData/"+stock + "_2.csv"
text_file = open(filename, "w")
text_file.write(r.content)
text_file.close()

with open(filename, 'rb') as original:
    reader = csv.DictReader(original, fieldnames=fieldnames)
    next(reader, None)  # skip the headers
    with open(newfile, 'w') as mod:
        writer = csv.writer(
            mod,
            delimiter=',',
            quotechar='|',
            quoting=csv.QUOTE_MINIMAL
        )

        for row in reader:

            dateArray = row['timestamp'].split("-")
            date = int(dateArray[0] + dateArray[1] + dateArray[2])

            # print("[WARNING] " + str(date) + " " + str(fromDate) + " " + str(toDate) + " " + str(date >= fromDate and date <= toDate))

            if date >= fromDate and date <= toDate:
                # (close-open)*100
                change_pct = (float(row['adjusted_close']) - float(row['open'])) / float(row['open']) * 100
                modRow = [
                    row['timestamp'],
                    row['open'],
                    row['high'],
                    row['low'],
                    row['close'],
                    row['volume'],
                    row['adjusted_close'],
                    stock,
                    round(change_pct, 4),
                ]
                writer.writerow(modRow)

        mod.close()
