//COPY paste from some other source

#include <stdio.h>

#ifndef DATATYPE
#define DATATYPE int
#endif

#ifndef FMT
#define FMT "%d "
#endif


// Function to swap two elements
void swap(DATATYPE* a, DATATYPE* b)
{
    DATATYPE temp = *a;
    *a = *b;
    *b = temp;
}

// Partition function
int partition(DATATYPE arr[], int low, int high)
{

    // initialize pivot to be the first element
    DATATYPE pivot = arr[low];
    int i = low;
    int j = high;

    while (i < j) {

        // condition 1: find the first element greater than
        // the pivot (from starting)
        while (arr[i] <= pivot && i <= high - 1) {
            i++;
        }

        // condition 2: find the first element smaller than
        // the pivot (from last)
        while (arr[j] > pivot && j >= low + 1) {
            j--;
        }
        if (i < j) {
            swap(&arr[i], &arr[j]);
        }
    }
    swap(&arr[low], &arr[j]);
    return j;
}

// QuickSort function
void quickSort(DATATYPE arr[], int low, int high)
{
    if (low < high) {

        // call Partition function to find Partition Index
        int partitionIndex = partition(arr, low, high);

        // Recursively call quickSort() for left and right
        // half based on partition Index
        quickSort(arr, low, partitionIndex - 1);
        quickSort(arr, partitionIndex + 1, high);
    }
}

// driver code
int main()
{
    DATATYPE arr[9] = { 19, 17, 15, 12, 16, 18, 4, 11, 13 };
    int n = sizeof(arr) / sizeof(arr[0]);

    // printing the original array
    printf("Original array: ");
    for (int i = 0; i < n; i++) {
        printf(FMT, arr[i]);
    }

    // calling quickSort() to sort the given array
    quickSort(arr, 0, n - 1);

    // printing the sorted array
    printf("\nSorted array: ");
    for (int i = 0; i < n; i++) {
        printf(FMT, arr[i]);
    }

    return 0;
}