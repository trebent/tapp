package handler

import (
	"slices"
	"testing"

	"github.com/trebent/tapp-backend/model"
)

func TestSortTappsByTime(t *testing.T) {
	tapps := []*model.Tapp{
		{Time: 1000},
		{Time: 5000},
		{Time: 2000},
		{Time: 3000},
		{Time: 4000},
	}

	slices.SortFunc(tapps, func(a, b *model.Tapp) int {
		return int(b.Time - a.Time)
	})

	expectedOrder := []int64{5000, 4000, 3000, 2000, 1000}
	for i, tapp := range tapps {
		if tapp.Time != expectedOrder[i] {
			t.Errorf("Expected tapp at index %d to have Time %d, but got %d", i, expectedOrder[i], tapp.Time)
		}
	}
}
